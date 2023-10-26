///usr/bin/env jbang "$0" "$@" ; exit $? 

//DEPS io.smallrye.beanbag:smallrye-beanbag-maven:1.3.2
//DEPS org.apache.maven:maven-resolver-provider:3.9.5
//DEPS org.apache.maven.resolver:maven-resolver-api:1.9.16
//DEPS org.apache.maven.resolver:maven-resolver-util:1.9.16
//DEPS org.apache.maven.resolver:maven-resolver-transport-http:1.9.16
//DEPS org.apache.maven.resolver:maven-resolver-transport-file:1.9.16
//DEPS org.ow2.asm:asm:9.6
//DEPS org.jgrapht:jgrapht-core:1.5.2

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.smallrye.beanbag.maven.MavenFactory;

class ListStaticInit { 

    public static void main(String[] args) throws SettingsBuildingException, ArtifactResolutionException, IOException, DependencyResolutionException {
    	String dependency = args[0];
    	String pattern = args[1];
    	
    	System.out.println("Resolving");
    	MavenFactory mavenFactory = MavenFactory.create();
    	RepositorySystem system = mavenFactory.getRepositorySystem();
    	ArtifactRequest request = new ArtifactRequest();
    	Artifact artifact = new DefaultArtifact(dependency);
		request.setArtifact(artifact);
		Settings settings = mavenFactory.createSettingsFromContainer(MavenFactory.getGlobalSettingsLocation(), MavenFactory.getUserSettingsLocation(), null);
		RepositorySystemSession session = mavenFactory.createSession(settings);
		DependencyFilter filter = new DependencyFilter() {
			@Override
			public boolean accept(DependencyNode node, List<DependencyNode> parents) {
				return true;
			}
		};
		CollectRequest collectRequest = new CollectRequest();
		collectRequest.setRoot(new Dependency(artifact, "compile"));
		collectRequest.setRepositories(MavenFactory.createRemoteRepositoryList(settings));
		DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, filter);
		DependencyResult dependencies = system.resolveDependencies(session, dependencyRequest);
		System.out.println("Dependencies");
		for (ArtifactResult depResult : dependencies.getArtifactResults()) {
			System.out.println(" "+depResult);
		}
		System.out.println("Scanning");
        Graph<String, DefaultEdge> callGraph = new DefaultDirectedGraph<>(DefaultEdge.class);
		Set<String> clInit = new HashSet<>();
        for (ArtifactResult depResult : dependencies.getArtifactResults()) {
			scanClassInit(depResult.getArtifact().getFile(), callGraph, clInit);
		}
		System.out.println("Filtering");
        for (String klass : clInit) {
			String method = klass+"|<clinit>|()V";
			Set<String> matches = new HashSet<>();
			findCallsMatchingPattern(method, callGraph, new HashSet<>(), pattern, matches);
			if(!matches.isEmpty()) {
				System.out.println("Class "+klass+" has static init calls matching:");
				for (String match : matches) {
					System.out.println(" "+match);
				}
			}
		}
		System.out.println("Done, with "+callGraph.vertexSet().size()+" vertexes");
    }

	private static void findCallsMatchingPattern(String startingPoint, Graph<String, DefaultEdge> callGraph, HashSet<String> visited, String pattern, Set<String> matches) {
		if(!visited.add(startingPoint)) {
			return;
		}
		if(startingPoint.startsWith(pattern)) {
			matches.add(startingPoint);
		}
		Set<DefaultEdge> outgoingEdges = callGraph.outgoingEdgesOf(startingPoint);
		for (DefaultEdge edge : outgoingEdges) {
			String target = callGraph.getEdgeTarget(edge);
			findCallsMatchingPattern(target, callGraph, visited, pattern, matches);
		}
	}

	private static void scanClassInit(File file, Graph<String, DefaultEdge> callGraph, Set<String> clInit) throws IOException {
		try (JarInputStream is = new JarInputStream(new FileInputStream(file))) {
			JarEntry jarEntry;
			while((jarEntry = is.getNextJarEntry()) != null) {
				if(jarEntry.getName().endsWith(".class")) {
					scanClass(is, callGraph, clInit);
				}
			}
		}
	}

	private static void scanClass(InputStream is, Graph<String, DefaultEdge> callGraph, Set<String> clInit) throws IOException {
		ClassReader reader = new ClassReader(is);
		reader.accept(new ClassVisitor(Opcodes.ASM9) {
			private String owner;

			@Override
			public void visit(int version, int access, String name, String signature, String superName,
					String[] interfaces) {
				super.visit(version, access, name, signature, superName, interfaces);
				this.owner = name;
			}
			
			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
					String[] exceptions) {
				String method = owner+"|"+name+"|"+descriptor;
				callGraph.addVertex(method);
				if(method.endsWith("|<clinit>|()V")) {
					clInit.add(owner);
				}
				
				MethodVisitor ret = super.visitMethod(access, name, descriptor, signature, exceptions);
				return new MethodVisitor(Opcodes.ASM9, ret) {
					public void visitMethodInsn(int opcode, String methodOwner, String name, String descriptor, boolean isInterface) {
						if(!owner.equals(methodOwner)) {
							// implicit class load
							addEdge(callGraph, method, methodOwner+"|<clinit>|()V");
						}
						// explicit call
						addEdge(callGraph, method, methodOwner+"|"+name+"|"+descriptor);
					}
					private void addEdge(Graph<String, DefaultEdge> callGraph, String start, String target) {
						callGraph.addVertex(target);
						callGraph.addEdge(start, target);
					}
					public void visitFieldInsn(int opcode, String fieldOwner, String name, String descriptor) {
						if(!owner.equals(fieldOwner)) {
							// implicit class load
							addEdge(callGraph, method, fieldOwner+"|<clinit>|()V");
						}
					}
				};
			}
		}, 0);
		
	}
}

