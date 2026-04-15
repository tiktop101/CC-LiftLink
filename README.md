# CC:LiftLink

 * CC:LiftLink by Tom
 * Copyright (c) 2026 Tom
 * Licensed under the Mozilla Public License 2.0
 */

Forge 1.20.1 mod which exposes Create elevator **redstone contacts** as a CC:Tweaked peripheral named `create_elevator`.

Pulley tracking is optional. The main setup only needs one linked elevator contact to work.

## Included
- full source
- `gradlew`
- `gradlew.bat`
- `gradle/wrapper/*`
- local `libs/create-1.20.1-6.0.8.jar`

## Build
```bash
./gradlew build
```

Built jar ends up in:
```bash
build/libs/
```

## Peripheral methods
- ```listFloors()```
- ```callToFloor(name)```
- ```callToY(y)```
- ```getState()``` 
- ```getFloors()``` 
- ```getCurrentLevel()``` 
- ```getTargetLevel()``` 
- ```getTargetY()``` 
- ```isMoving()``` 
- ```isActive()``` 
- ```getCabY()```
- ```getSpeed()```
- ```getDirection()```
- ```getColumnInfo()```
- ```getCurrentFloor()```
- ```getNearestFloor()``` 
- ```hasFloor(name)``` 
- ```isAtFloor(name)```
