[![RaiMan's Stuff](https://raw.github.com/RaiMan/SikuliX-2014-Docs/master/src/main/resources/docs/source/RaiManStuff64.png)](http://www.sikuli.org) SikuliX2 (version 2.0.x)
============

[![Join the chat at https://gitter.im/RaiMan/SikuliX2](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/RaiMan/SikuliX2?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

<hr>
**Version 2.0.0 under developement** (starting somewhen in March 2015)
<hr>
**[latest stable version is 1.1.0](https://github.com/RaiMan/SikuliX-2014)**
<hr>

**Forking and/or downloading this repo only makes sense:**

 - if you want to get a knowledge about the internals of Sikuli
 - if you want to create your own packages containing Sikuli features
 - if you want to contribute.

**In any case: be sure you know the content of the [SikuliX Quickstart](http://www.sikulix.com/quickstart.html)**

<hr>

Maven module structure for developement
---

**sikulix2api**

the API to be used in Java and Java aware scripting languages

**sikulix2** (inactive as module currently)

the Sikuli IDE and the support for running scripts in a supported scripting language.

**sikulixtensions** (inactive as module currently)

Packages that extend the SikuliX features in various ways.<br>
On the usage and/or developement level they depend on the core features of sikulixapi or sikulix.

---

Prerequisites for development
---

 - a Java JDK 1.7+
 - Maven 3+

I use **NetBeans 8+** for developement.
