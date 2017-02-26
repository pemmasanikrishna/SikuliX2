var aPicture = Do.picture("sikulix2");
Do.print("picture: %s (valid: %s)", aPicture, aPicture.isValid());
var match = Do.wait(aPicture);
Do.on().showMatch();