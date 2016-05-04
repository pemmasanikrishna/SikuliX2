[![RaiMan's Stuff](https://raw.github.com/RaiMan/SikuliX-2014-Docs/master/src/main/resources/docs/source/RaiManStuff64.png)](http://www.sikuli.org) SikuliX2 (version 2.0.x)
============

[![Join the chat at https://gitter.im/RaiMan/SikuliX2](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/RaiMan/SikuliX2?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

<hr>
**Version 2.0.0-Beta under developement**<br> 
built on **[TravisCI](https://travis-ci.org/RaiMan/SikuliX2)** (coming soon)
<hr>
**[latest working version is 1.1.1](https://github.com/RaiMan/SikuliX-2014)**
<hr>

**Forking and/or downloading this repo only makes sense:**

 - if you want to get a knowledge about the internals of Sikuli
 - if you want to create your own packages containing Sikuli features
 - if you want to contribute.

<hr>

Maven module structure for developement
---

**sikulix2api**

the API to be used in Java and Java aware scripting languages

**sikulix** 

the SikuliX IDE (internally using **[jEdit 5.3.0](http://www.jedit.org))**
 
 - full featured multi-purpose editor
 - SikuliX image handling added via jEdit plugin
 - run scripts (scripting supported internally: JavaScript, (J)Python, (J)Ruby, RobotFramework)
 - everyone may simply add whatever feature needed based on the possibilities of jEdit

**sikulixrun** 

run SikuliX scripts locally or remote (used by SikuliX IDE to run supported scripts)

**sikulix2apitest**

basic api tests for developement (later there will be a seperate regression test package)

---

Prerequisites for development
---

 - a Java JDK 1.7+
 - Maven 3+
 - only 64-Bit Systems supported

I use **[IntelliJ IDEA CE](https://www.jetbrains.com/idea/)** for developement.
