# Find calls to certain packages from static init

Sometimes `native-image` will complain about static init starting threads or something annoying,
and this requires you to mark some classes as requiring to be initialised at run time instead of
build time. But finding which classes to mark can be a burden.

If you want to find what classes are making static init calls to some packages, you can use this
tool.

## Installation

Install JBang: https://www.jbang.dev/download/

```shell
$ jbang app install static-init-finder@fromage
```

## Running

Find static init references from openhtml-java2d to the java/awt package:

```shell
$ static-init-finder com.openhtmltopdf:openhtmltopdf-java2d:1.0.10 'java/awt'
Resolving
SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
Dependencies
 com.openhtmltopdf:openhtmltopdf-java2d:jar:1.0.10 < central (https://repo1.maven.org/maven2, default, releases)
 com.openhtmltopdf:openhtmltopdf-core:jar:1.0.10 < central (https://repo1.maven.org/maven2, default, releases)
Scanning
Filtering
Class com/openhtmltopdf/swing/AWTFSImage$NullImage has static init calls matching:
 java/awt/GraphicsConfiguration|<clinit>|()V
 java/awt/Graphics2D|fillRect|(IIII)V
 java/awt/image/BufferedImage|<init>|(III)V
 java/awt/Graphics2D|setComposite|(Ljava/awt/Composite;)V
 java/awt/GraphicsConfiguration|createCompatibleImage|(III)Ljava/awt/image/BufferedImage;
 java/awt/GraphicsEnvironment|getLocalGraphicsEnvironment|()Ljava/awt/GraphicsEnvironment;
 java/awt/GraphicsDevice|<clinit>|()V
 java/awt/Graphics2D|<clinit>|()V
 java/awt/image/BufferedImage|<clinit>|()V
 java/awt/GraphicsDevice|getDefaultConfiguration|()Ljava/awt/GraphicsConfiguration;
 java/awt/AlphaComposite|<clinit>|()V
 java/awt/GraphicsEnvironment|isHeadlessInstance|()Z
 java/awt/GraphicsEnvironment|<clinit>|()V
 java/awt/Color|<init>|(IIII)V
 java/awt/GraphicsEnvironment|getDefaultScreenDevice|()Ljava/awt/GraphicsDevice;
 java/awt/image/BufferedImage|createGraphics|()Ljava/awt/Graphics2D;
 java/awt/Graphics2D|setColor|(Ljava/awt/Color;)V
 java/awt/Graphics2D|dispose|()V
 java/awt/Color|<clinit>|()V
Class com/openhtmltopdf/swing/AWTFSImage has static init calls matching:
 java/awt/GraphicsConfiguration|<clinit>|()V
 java/awt/Graphics2D|fillRect|(IIII)V
 java/awt/image/BufferedImage|<init>|(III)V
 java/awt/Graphics2D|setComposite|(Ljava/awt/Composite;)V
 java/awt/GraphicsConfiguration|createCompatibleImage|(III)Ljava/awt/image/BufferedImage;
 java/awt/GraphicsEnvironment|getLocalGraphicsEnvironment|()Ljava/awt/GraphicsEnvironment;
 java/awt/GraphicsDevice|<clinit>|()V
 java/awt/Graphics2D|<clinit>|()V
 java/awt/image/BufferedImage|<clinit>|()V
 java/awt/GraphicsDevice|getDefaultConfiguration|()Ljava/awt/GraphicsConfiguration;
 java/awt/AlphaComposite|<clinit>|()V
 java/awt/GraphicsEnvironment|isHeadlessInstance|()Z
 java/awt/GraphicsEnvironment|<clinit>|()V
 java/awt/Color|<init>|(IIII)V
 java/awt/GraphicsEnvironment|getDefaultScreenDevice|()Ljava/awt/GraphicsDevice;
 java/awt/image/BufferedImage|createGraphics|()Ljava/awt/Graphics2D;
 java/awt/Graphics2D|setColor|(Ljava/awt/Color;)V
 java/awt/Graphics2D|dispose|()V
 java/awt/Color|<clinit>|()V
Done, with 6546 vertexes
```

This shows you you have at least two classes from this module that make static init calls to Java2D that
you may want to initialise at run time.
