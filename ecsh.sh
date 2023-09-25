#!/bin/bash
PRGDIR=`dirname "$0"`
echo $PRGDIR
if [ "$#" -eq 0 ]
then
  java -cp $PRGDIR Ecsh
else
  java -cp $PRGDIR Ecsh $1
fi
