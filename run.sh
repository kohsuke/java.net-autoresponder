#!/bin/sh
cd /files/java.net-autoresponder

admin=kohsuke.kawaguchi@sun.com

# figure out which locale is requested
locale=$(echo $RECIPIENT | /bin/sed "s/kk122374-javanet-autoresponder\(.*\)@.*/\\1/" | /bin/sed "s/-/_/g")
if [ -e autoreply$locale.txt ];
then
  # handle it
  /files/hudson/hudson "java.net-autoresponder" java -jar build/autoresponder.jar "$admin" autoreply$locale.txt >> log 2>&1
else
  # no such locale. maybe a new user registration?
  cat | /bin/mail $admin >> log 2>&1
fi
