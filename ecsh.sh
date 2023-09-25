#!/bin/bash
if [ "$#" -eq 0 ]
then
  java Ecsh
else
  java Ecsh "$1"
fi
