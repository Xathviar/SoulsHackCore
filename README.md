# SoulsHackCore
Map Generator for an Ascii Roguelike Game. \
This Dungeon Generator even works with Seeds. \
\
Generates the map as a .tmx File which is xml and used by [Tiled](https://www.mapeditor.org/). \
\
You need the files ami8.png and ami8.tsx for it to work, and both of these files have to be in the same directory as the generated tmx File.

## Syntax to generate World and generate a tmx File
```java
WorldGenerator gen = new WorldGenerator(); 
gen.createTiledMap(new File("test.tmx"));
```
## Syntax to generate World and output it as a StringBuilder
```java
WorldGenerator gen = new WorldGenerator(); 
StringBuilder s = gen.generateTiledMap();
```

## Include it in your Maven Repository
```maven
	<repositories>
		<repository>
			<id>jitpack.io</id>
			<url>https://jitpack.io</url>
		</repository>
	</repositories>
  
  <dependencies>
  		<dependency>
			<groupId>com.github.Xathviar.SoulsHackCore</groupId>
			<artifactId>MapGen</artifactId>
			<version>v.1.3</version>
		</dependency>
  </dependencies>
```
