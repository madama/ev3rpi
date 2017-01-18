#!/bin/bash
java -cp "target/*:target/lib/*" -Djava.library.path=/usr/lib/jni org.danysoft.ev3rpi.RobotUI
