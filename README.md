# jpigpio
A Java interface to the Raspberry Pi pigpio library

[Pigpio](http://abyz.co.uk/rpi/pigpio/index.html) is a great library for accessing the GPIO pins and other inputs and outputs of a Raspberry Pi.  Pigpio provides interfaces for C and Python.  However, if what you want to do is use the functions of the library from a Java application, you are stuck.  The purpose of this project is to provide a Java interface to the pigpio library.

The core of the solution is a Java interface called `jpigpio.JPigpio`.  It is this interface that provides the exposed functions that perform the pigpio requests.  Currently, the following pigpio operations are supported:

* gpioInitialize
* gpioTerminate
* gpioGetMode
* gpioSetMode
* gpioRead
* gpioWrite
* gpioTrigger
* gpioSetAlertFunc (Not available for sockets)
* gpioSetPullUpDown
* gpioDelay
* gpioTick
* gpioShiftOut (Extra)

* gpioServo

* i2cOpen
* i2cClose
* i2cReadDevice
* i2cWriteDevice

* spiOpen
* spiClose
* spiRead
* spiWrite
* spiXfer

Obviously, this is only a subset of the full and rich capabilities of C and Python pigpio.  However, the subset is the starter set we chose to create.  If you find you need a pigpio function that is not provided by Java, simply let us know and we will turn it around very quickly.  We simply don't want to spend time porting if there is no demand.

Since `jpigpio.JPigpio` is a Java interface, something must implement it.  Two separate implementations are provided.  One called `jpigpio.Pigpio` and one called `jpigpio.PigpioSocket`.  Both of them implement the `jpigpio.JPigpio` interface.  The difference between them is how the calls from Java to pigpio are achieved.

## jpigpio.Pigpio
Using this class, your custom Java code **must** be executed on the Raspberry Pi.  The method calls made in Java are passed as quickly as possible to the underlying pigpio C library for execution.  This provides the fastest capability to call pigpio with as little Java overhead as possible.  Of course, the Java classes must execute on the Pi.

![text](images/NoSockets.png)  

## jpigpio.PigpioSocket
Using this class, your custom Java code can run either on the Raspberry Pi or on a separate machine as long as there is a network connection (TCP/IP).  The pigpio function requests are transmitted via sockets to the pigpio supplied demon which is called `pigpiod` which can listen for incoming requests and service them when they arrive.

![text](images/Sockets.png)  

## Exception handling
The pigpio library returns code values which indicate the outcome of a function call.  In Java, we have the ability to throw exceptions.  As such, if an error is detected when making a jpigpio method call, an exception of type `PigpioException` is thrown.  This makes our logic for error handling much cleaner as we do not have to explicitly check the response values for each of the calls.

Upon catching a PigpioException, we can ask for the error code value with the `getErrorCode()` method.

Symbolic definitions for each of the potential errors are supplied as statics on the PigpioException class.  For example:

     PigpioException.PI_BAD_GPIO

will have a value of `-3` which is the underlying code for the corresponding pigpio error.

    try {
    	// Perform a pigpio function
    }
    catch(PigpioException e) {
    	e.printStackTrace();
    	if (e.getErrorCode() == PigpioException.PI_BAD_GPIO) {
    		System.out.println("You supplied a bad pin!");
    	}
    }

## Alert callbacks
The `gpioSetAlertFunc` method takes a gpio pin and an instance of an `Alert` class.  The `Alert` is a Java interface that has the following signature:

	public interface Alert {
		public void alert(int gpio, int level, long tick);
	}

This is a callback class.  When the state of the gpio pin changes, the `alert` method is invoked to indicate that a state change event has occurred.  Because the `Alert` interface only has a single member function, it is eligible to be used as a Java 8 lambda function which makes it very convenient to use:

	pigpio.gpioSetAlertFunc(TESTPIN, (gpio, level, tick) -> {
		System.out.println(
			String.format("Callback in Java: We received an alert on: %d with %d at %d",
				gpio, level, tick));
	});

## Utilities
A class called `Utils` provides some Java utilities that can be used in conjunction with JPigpio.

* `static void addShutdown(JPigpio pigpio)` - Register a JVM shutdown handler that automatically gets called when the JVM ends.  This shutdown handler cleans up (terminates) any resources allocated on behalf of pigpio.

## Constant definitions

The JPigpio interface defines a set of constants for use with the library:

* PI_HIGH - A high value
* PI_LOW - A low value
* PI_ON - A high value
* PI_OFF - A low value
* PI_INPUT - The mode of a gpio for input
* PI_OUTPUT - The mode of a gpio for output
* PI\_PUD_OFF - No pull-up associated with the gpio
* PI\_PUD_UP - Pull-up the gpio to high
* PI\_PUD_DOWN - Pull-down the gpio to low

----

# Running an application
JPigpio is built against Java version 8 and hence requires a Java 8 environment in order to run.  This is the current level of Java supplied with Raspbian.  An application that only uses the `PigpioSocket` class needs no additional special environment.  However, an application that uses the `Pigpio` class needs to be able to find the JPigpio shared library written in C.  This is specified by adding the path to the directory which contains the file `libJPigpioC.so`.

    java -Djava.library.path=<directory containing JPigpio library> tests/Test_Blink

If the library can not be found, you will get an exception that looks similar to:

	Exception in thread "main" java.lang.UnsatisfiedLinkError: no JPigpioC in java.library.path
	        at java.lang.ClassLoader.loadLibrary(ClassLoader.java:1857)
	        at java.lang.Runtime.loadLibrary0(Runtime.java:870)
	        at java.lang.System.loadLibrary(System.java:1119)
	        at jpigpio.Pigpio.<clinit>(Pigpio.java:5)
	        at tests.Test_Blink.run(Test_Blink.java:30)
	        at tests.Test_Blink.main(Test_Blink.java:24)

Check that the java.library.path is supplied and that its value points to a directory which contains the `libJPigpioC.so` library file.

----

# Installation
A prerequisite of this package is the correct installation of pigpio on the Raspberry Pi by itself.  Please see the Pigpio [Download & Install](http://abyz.co.uk/rpi/pigpio/download.html) page for details.  A quick way to check that a version of pigpio is present is to look for the following files:

* `/usr/local/lib/libpigpio.a`
* `/usr/local/bin/pigpiod`
* `/usr/local/include/pigpio.h`

Details of the installation techniques for this project to be provided here ...

1. Download to pi: wget https://github.com/nkolban/jpigpio/archive/master.zip
2. Compile java code to .class files:
* unzip master.zip
* cd jpigpio-master
* cd jpigpio
* mkdir bin
* javac $(find . -name "*.java") -d bin
2. Change JPigpio/Makefile install dir: i.e. /home/pi
3. Change JPigpioC/Makefile to match above: i.e. /home/pi
4. Make sure JAVA_HOME is set (mine wasn't). Added it to makefile.
5. Run make from jpigpio-master
----
# Mapping
On occasion, you may find existing code written for either an Arduino (a sketch) or for alternate Raspberry Pi libraries such as Wiring Pi.  Here we provide a mapping from similar capabilities to those found in the JPigpio package.

## Arduino

* digitalWrite - gpioWrite
* digitalRead - gpioRead
* shiftOut - gpioShiftOut
* pinMode - gpioSetMode

## Wiring Pi

* digitalWrite - gpioWrite
* digitalRead - gpioRead
* pinMode - gpioSetMode

----

# Future of the project
I will be delighted to accept change requests and bug reports (if any can be found) and turn those around as quickly as possible.  I have been an IT hobbyist/coder for decades and am not planning on going anywhere soon so feel free to believe that this will be a maintained project as long as needed.
