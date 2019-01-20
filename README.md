# Readme
The http-API for calling sequences runs on port 8000. 
The light sequences are defined in the sequences.json file. 

## Allowed values for variables in sequences.json
	
| Key                                       | Description                                                          |
|-------------------------------------------|----------------------------------------------------------------------|
| "type": "light" &#124; "sleep"            | Action type. Use light-id, r, g, b, transitionTime or light-id, h, s, v, transitionTime for type light. Use duration for type sleep. |
| "duration": 0-2147483647			            | Do nothing for x milliseconds                                        |
| "light-id": 0-(lightCount - 1) &#124; -1  | -1 is for selecting all lights                                       | 
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