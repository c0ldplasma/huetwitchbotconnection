Allowed values for variables in sequences.json
---------------------------------------------------

"type": "light" | "sleep"\
	for "light" -> "light-id" & "r" & "g" & "b" & "transitionTime"\
			or  -> "light-id" & "h" & "s" & "v" & "transitionTime"\
	for "sleep" -> "duration"\

"duration": 0-2147483647			   // do nothing for x milliseconds\
"light-id": 0-(lightCount - 1) | -1	   // -1 is for selecting all lights\
"r" "g" "b": 0-255 | -1                // -1 is for setting the value from before executing the sequence\
"h": 0-360							   // -1 is for setting the value from before executing the sequence\
"s" "v": 0.0-1.0					   // -1 is for setting the value from before executing the sequence\
"transitionTime": 0,1,2,3,4....        // transition time in 100ms steps\