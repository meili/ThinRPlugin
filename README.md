## ThinR gradle plugin
* Other languages: [简体中文](README.zh-cn.md).



### ThinR plugin introduce
***


This tool will remove all the class in R.java except the styleable class and replace the referance into the constant value. So you can reduce the dex files number and apk size.

The plugin has been used on the mogujie app, the apk size is reduced by 1MB (the original apk size of 40MB), the number of DEX reduced by 3.

### ThinR plugin principle


In the R.java of android project , except the class styleable , all the class fields are int object and will never change the value in the run-time.

So in the compile-time we mark all the fields' name and their values,then use the asm tool to scan all the class files to replace the name into the value.


### HOW TO USE
***
Add the dependency in the build.gralde of the project

 	classpath   'com.mogujie.gradle:ThinRPlugin:0.0.3'
 
Add the following code in the inner gradle file of the module :

	 apply plugin: 'thinR'
	 
	 thinR {
	     // In order not to affect the daily development of compilation speed, debug version can not delete R
	   skipThinRDebug = true
	 }

If you are using Proguard, please keep the R class in the confusion file.

	
	-keepclassmembers class **.R$* {
		 public static <fields>;
	}
	-keep class **.R {*;}
	-keep class **.R$* {*;}
	-keep class **.R$*
	-keep class **.R
	
if you are using ButterKnife either, please keep the R2 class in the confusion file.


    -keepclassmembers class **.R2$* {
		 public static <fields>;
	}
    -keep class **.R2 {*;}
    -keep class **.R2$* {*;}
    -keep class **.R2$*
    -keep class **.R2
	


### Attention
***
The plugin does not support the pre-dex compilation model (i.e., one that must be turned on in multidex or Proguard)
	    
### Licence
***
ThinRPlugin is licensed under the MIT license




In case of using the issue to the wangzhi@meili-inc.com please!

