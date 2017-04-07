[![RaiMan's Stuff](https://raw.github.com/RaiMan/SikuliX-2014-Docs/master/src/main/resources/docs/source/RaiManStuff64.png)](http://sikulix.com) SikuliX2 (version 2.0.x)
============

**[latest useable version is 1.1.1](https://github.com/RaiMan/SikuliX-2014)**

[![Join the chat at https://gitter.im/RaiMan/SikuliX2](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/RaiMan/SikuliX2?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

<hr>
**Version 2.0.0 under developement**<br>
[![Build Status](https://travis-ci.org/RaiMan/SikuliX2.svg?branch=master)](https://travis-ci.org/RaiMan/SikuliX2)
<br><br>
Translation project: [Transifex::SikuliX2](https://www.transifex.com/sikulix/sikulix2/dashboard/)
<br><br>
Documentation project: [ReadTheDocs::SikuliX2](http://sikulix2.readthedocs.org/) based on [GitHub::SikuliX2-Docs](https://github.com/RaiMan/SikuliX2-Docs)
<hr>
**Forking and/or downloading this repo only makes sense:**

 - if you want to get a knowledge about the internals of Sikuli
 - if you want to create your own packages containing Sikuli features
 - if you want to contribute.

<hr>

**BE AWARE** The use for scripting and Java programming is only possible after the first nightly builds are available. Currently you might fork the project and make your tests in the project context. Issues and pull requests are only accepted here on Github.

<hr>

Maven module structure for developement
---

**sikulixapi**

the API to be used in Java and Java aware scripting languages

 - the new features will be in **com.sikulix.core** and **com.sikulix.api**
 - the official API in **org.sikuli.script** will be kept for backwards compatibility, but rerouted to **com.sikulix.api** as needed
 - new projects using Java should use the API as provided by **com.sikulix.api**

**sikulixcoretest**

basic tests for developement of the new packages **com.sikulix.core** and **com.sikulix.api**

**sikulixapitest**

(inactive) basic tests for **org.sikuli.script** as regression test (will later be a seperate package)

**sikulix**

the SikuliX IDE (internally using **[jEdit 5.3.0](http://www.jedit.org))**

 - full featured multi-purpose editor
 - SikuliX image handling added via jEdit plugin
 - run scripts (scripting supported internally: JavaScript, (J)Python, (J)Ruby, RobotFramework)
 - everyone may simply add whatever feature needed based on the possibilities of jEdit
 - 
 For tests in the project context the jEdit package has to be [downloaded from here](http://download.sikulix.com/SXEditor.zip), unzipped into the folder `SikulixAppData/SX2/Extension`, so you finally have a folder `SikulixAppData/SX2/Extension/SXEditor`.

---

Prerequisites for development and testing
---

 - a Java JDK 1.7+
 - Maven 3+
 - only 64-Bit Systems supported

**For developement I use the [JetBrains IDEs](https://www.jetbrains.com)**

 - **[IntelliJ IDEA CE](https://www.jetbrains.com/idea/)** for Java and everything else
 - **[PyCharm CE](https://www.jetbrains.com/pycharm/)** for special Jython/Python stuff
 - **[RubyMine](https://www.jetbrains.com/ruby/)** for special JRuby/Ruby stuff (special license for OpenSource projects)
 
Be aware for development and testing
---

 - the so called folder `SikulixAppData`, where SikuliX specific stuff is stored once per machine, for SikuliX2 now is `SikulixAppData/SX2` to allow the parallel usage of SikuliX1 and SikuliX2 on the same machine.
