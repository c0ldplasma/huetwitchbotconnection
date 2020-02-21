# Hue Twitch-Bot Connection
The http-API for calling sequences runs on port 8000. The response to every http request to the API is defined in the response.txt file (default is a single space character).
The light sequences are defined in the sequences.json file. 

## Download
https://github.com/c0ldplasma/huetwitchbotconnection/releases

## Defining Sequences in sequences.json
	
| Key                                       | Description                                                          |
|-------------------------------------------|----------------------------------------------------------------------|
| "type": "light" &#124; "sleep"            | Action type. Use light-id, r, g, b, transitionTime or light-id, h, s, v, transitionTime for type light. Use duration for type sleep. |
| "duration": 0-2147483647			            | Do nothing for x milliseconds                                        |
| "light-id": 0 to (lightCount - 1) &#124; -1  | -1 is for selecting all lights                                       | 
| "r" "g" "b": 0-255 &#124; -1              | -1 is for setting the value from before executing the sequence       |
| "h": 0-360						                    | -1 is for setting the value from before executing the sequence       |
| "s" "v": 0.0-1.0					                | -1 is for setting the value from before executing the sequence       |
| "transitionTime": 0,1,2,3,4....           | Transition time in 100ms steps                                       |

### Example
```json
{
  "sequences":{
    "sequenceName1": [
      { "type": "light", "light-id": 1, "r": 0, "g": 0, "b": 0, "transitionTime": 0},
      { "type": "sleep", "duration": 5000},
      { "type": "light", "light-id": 1, "r": -1, "g": -1, "b": -1, "transitionTime": 500}
    ],
    "sequenceName2":	[
      { "type": "light", "light-id": 1, "r": 255, "g": 255, "b": 255, "transitionTime": 0},
      { "type": "sleep", "duration": 50},
      { "type": "light", "light-id": 1, "r": -1, "g": -1, "b": -1, "transitionTime": 0}
    ]
  }
}
```

## Deployment
### Java 11+

The changes in the licence model and the distribution of java made by oracle made it necessary to bundle a Java JRE with the Application. Furthermore it is not possible to use the OracleJDK anymore. Instead it is needed to use a OpenJDK version. Since Java 11 OracleJDK and OpenJDK are functionally identical, but differ in the license. Basically OracleJDK is now only for paid customers of Oracle while OpenJDK is completely free to use. (no claim for correctness)

#### Building the custom JRE

A custom JRE can be built with the jlink tool included in the OpenJDK 11.

1. Download javafx-jmods from https://gluonhq.com/products/javafx/
1. Extract to a folder of your choice.
1. Add all needed modules to the following command.
1. jlink --module-path "[javafx-jmods folder]" --add-modules java.base,javafx.controls,jdk.httpserver --output jre-11-huedeepbot --strip-debug --compress 2 --no-header-files --no-man-pages
1. You now have your own jre in the jre-11-huedeepbot folder.

#### Building the application

Since version 0.0.13 the gradle build system is used. Gradle version 5.1.1.

1. To build the application first run the build task the the createExe task.
1. Copy the huesdk.dll from ./[project folder]/lib to ./[project folder]/build/launch4j/lib
1. Remove the huetwitchbotsconnection.jar in ./[project folder]/build/launch4j/lib
1. Copy the jre-11-huedeepbot folder to ./[project folder]/build/launch4j/lib
1. Copy the sequences.json and the response.txt from ././[project folder] to ./[project folder]/build/launch4j
1. Copy folder ./[project folder]/icons to ./[project folder]/build/launch4j/icons and remove the icon.ico file.
1. Create a folder named persistence in ./[project folder]/build/launch4j
1. Rename/copy the launch4j folder to HueDeepbot-[version] and zip it.

For debugging the HueDeepbot.exe change headerType from 'gui' to 'console' in the build.gradle file.

## License

This project is licensed under the MIT License. See LICENSE file. 
Exceptions are the Icon which is from https://www.iconfinder.com/icons/406712/rgb_icon and the Hue SDK binaries in the lib folder. The License of those binaries also can be found in the lib folder.
