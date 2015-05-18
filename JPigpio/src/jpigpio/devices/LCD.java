package jpigpio.devices;

import jpigpio.JPigpio;
import jpigpio.PigpioException;
import jpigpio.Utils;

public class LCD {
	private JPigpio pigpio;
	private int registerSelectGpio;
	private int readWriteGpio;
	private int enableGpio;
	private int db4Gpio;
	private int db5Gpio;
	private int db6Gpio;
	private int db7Gpio;
	
	// The display mode
	private byte displayMode;
	private byte functionMode;
	private byte entryMode;
	
	private final boolean RW_READ=true;
	private final boolean RW_WRITE=false;
	private final boolean RS_DATA=true;
	private final boolean RS_INSTRUCTION=false;
	
	// commands
	private final byte LCD_CLEARDISPLAY = 0x01;
	private final byte LCD_RETURNHOME = 0x02;
	private final byte LCD_ENTRYMODESET = 0x04;
	private final byte LCD_DISPLAYCONTROL = 0x08;
	private final byte LCD_CURSORSHIFT = 0x10;
	private final byte LCD_FUNCTIONSET = 0x20;
	private final byte LCD_SETCGRAMADDR = 0x40;
	private final byte LCD_SETDDRAMADDR = (byte)0x80;

	// flags for display entry mode
	private final byte LCD_ENTRYRIGHT = 0x00;
	private final byte LCD_ENTRYLEFT = 0x02;
	private final byte LCD_ENTRYSHIFTINCREMENT = 0x01;
	private final byte LCD_ENTRYSHIFTDECREMENT = 0x00;

	// flags for display on/off control
	private final byte LCD_DISPLAYON = 0x04;
	private final byte LCD_DISPLAYOFF = 0x00;
	private final byte LCD_CURSORON = 0x02;
	private final byte LCD_CURSOROFF = 0x00;
	private final byte LCD_BLINKON = 0x01;
	private final byte LCD_BLINKOFF = 0x00;

	// flags for display/cursor shift
	private final byte LCD_DISPLAYMOVE = 0x08;
	private final byte LCD_CURSORMOVE = 0x00;
	private final byte LCD_MOVERIGHT = 0x04;
	private final byte LCD_MOVELEFT = 0x00;

	// flags for function set
	private final byte LCD_8BITMODE = 0x10;
	private final byte LCD_4BITMODE = 0x00;
	private final byte LCD_2LINE = 0x08;
	private final byte LCD_1LINE = 0x00;
	private final byte LCD_5x10DOTS = 0x04;
	private final byte LCD_5x8DOTS = 0x00;

	public LCD(JPigpio pigpio, int registerSelect, int readWrite, int enable, int db4, int db5, int db6, int db7) throws PigpioException {
		this.pigpio = pigpio;
		this.registerSelectGpio = registerSelect;
		this.readWriteGpio = readWrite;
		this.enableGpio = enable;
		this.db4Gpio = db4;
		this.db5Gpio = db5;
		this.db6Gpio = db6;
		this.db7Gpio = db7;
		this.displayMode = LCD_DISPLAYCONTROL | LCD_DISPLAYON | LCD_CURSOROFF | LCD_BLINKOFF;
		this.functionMode = LCD_FUNCTIONSET | LCD_4BITMODE | LCD_1LINE | LCD_5x10DOTS;
		this.entryMode = LCD_ENTRYMODESET | LCD_ENTRYLEFT | LCD_ENTRYSHIFTDECREMENT;
		
		
		System.out.println(String.format("registerSelect: %d\nreadWrite: %d\nenable: %d\ndb4=%d\ndb5=%d\ndb6=%d\ndb7=%d", //
				registerSelect, readWrite, enable, db4, db5, db6, db7));
		pigpio.gpioWrite(enableGpio, false);
		init();
		//lines(2);
	} // End of constructor
	
	private void init() throws PigpioException {
		System.out.println("init >");
		pigpio.gpioDelay(50000);
		write4bits(RS_INSTRUCTION, RW_WRITE, (byte)0b11);
		pulseEnable();
		pigpio.gpioDelay(4500);
		write4bits(RS_INSTRUCTION, RW_WRITE, (byte)0b11);
		pulseEnable();
		pigpio.gpioDelay(4500);
		write4bits(RS_INSTRUCTION, RW_WRITE, (byte)0b11);
		pulseEnable();
		pigpio.gpioDelay(4500);
		write4bits(RS_INSTRUCTION, RW_WRITE, (byte)0b10);
		pulseEnable();
		functionModeSet();
		displayModeSet();
		entryModeSet();
	}

	public void clear() throws PigpioException {
		// +----+----+-----+-----+-----+-----+-----+-----+-----+-----+
		// | RS | RW | DB7 | DB6 | DB5 | DB4 | DB3 | DB2 | DB1 | DB0 |
		// +----+----+-----+-----+-----+-----+-----+-----+-----+-----+
		// | 0  | 0  |  0  |  0  |  0  |  0  |  0  |  0  |  0  |  1  |
		// +----+----+-----+-----+-----+-----+-----+-----+-----+-----+
		System.out.println("clear>");
		write(RS_INSTRUCTION, RW_WRITE, LCD_CLEARDISPLAY);
	} // End of clear

	public void home() throws PigpioException {
		// +----+----+-----+-----+-----+-----+-----+-----+-----+-----+
		// | RS | RW | DB7 | DB6 | DB5 | DB4 | DB3 | DB2 | DB1 | DB0 |
		// +----+----+-----+-----+-----+-----+-----+-----+-----+-----+
		// | 0  | 0  |  0  |  0  |  0  |  0  |  0  |  0  |  1  |  -  |
		// +----+----+-----+-----+-----+-----+-----+-----+-----+-----+
		System.out.println("home >");
		write(RS_INSTRUCTION, RW_WRITE, LCD_RETURNHOME);
	} // End of home

	private void displayModeSet() throws PigpioException {
		// +----+----+-----+-----+-----+-----+-----+-----+-----+-----+
		// | RS | RW | DB7 | DB6 | DB5 | DB4 | DB3 | DB2 | DB1 | DB0 |
		// +----+----+-----+-----+-----+-----+-----+-----+-----+-----+
		// | 0  | 0  |  0  |  0  |  0  |  0  |  1  |  D  |  C  |  B  |
		// +----+----+-----+-----+-----+-----+-----+-----+-----+-----+
		System.out.println("Display Mode >");
		write(RS_INSTRUCTION, RW_WRITE, displayMode);
	} // End of displayModeSet

	public void functionModeSet() throws PigpioException {
		// +----+----+-----+-----+-----+-----+-----+-----+-----+-----+
		// | RS | RW | DB7 | DB6 | DB5 | DB4 | DB3 | DB2 | DB1 | DB0 |
		// +----+----+-----+-----+-----+-----+-----+-----+-----+-----+
		// | 0  | 0  |  0  |  0  |  1  |  DL |  N  |  F  |  -  |  -  |
		// +----+----+-----+-----+-----+-----+-----+-----+-----+-----+
		// When DL=0, data is 4 bit.
		System.out.println("Function Mode >");
		write(RS_INSTRUCTION, RW_WRITE, functionMode);
	} // End of functionModeSet
	
	private void setDDRAMAddress(int address) throws PigpioException {
		// +----+----+-----+-----+-----+-----+-----+-----+-----+-----+
		// | RS | RW | DB7 | DB6 | DB5 | DB4 | DB3 | DB2 | DB1 | DB0 |
		// +----+----+-----+-----+-----+-----+-----+-----+-----+-----+
		// | 0  | 0  |  1  | AC6 | AC5 | AC4 | AC3 | AC2 | AC1 | AC0 |
		// +----+----+-----+-----+-----+-----+-----+-----+-----+-----+
		System.out.println("Set DDRAM Address>");
		write(RS_INSTRUCTION, RW_WRITE, (byte)(0b1000000 | (address & 0b1111111)));
	} // End of setDDRAMAddress
	
	private void entryModeSet() throws PigpioException {
		// +----+----+-----+-----+-----+-----+-----+-----+-----+-----+
		// | RS | RW | DB7 | DB6 | DB5 | DB4 | DB3 | DB2 | DB1 | DB0 |
		// +----+----+-----+-----+-----+-----+-----+-----+-----+-----+
		// | 0  | 0  |  0  |  0  |  0  |  0  |  0  |  1  | I/D |  S  |
		// +----+----+-----+-----+-----+-----+-----+-----+-----+-----+
		System.out.println("Set setEntryMode>");
		write(RS_INSTRUCTION, RW_WRITE, entryMode);
	} // End of entryModeSet
	
	private boolean isBusy() throws PigpioException {
		// +----+----+-----+-----+-----+-----+-----+-----+-----+-----+
		// | RS | RW | DB7 | DB6 | DB5 | DB4 | DB3 | DB2 | DB1 | DB0 |
		// +----+----+-----+-----+-----+-----+-----+-----+-----+-----+
		// | 0  | 1  |  BF | AC6 | AC5 | AC4 | AC3 | AC2 | AC1 | AC0 |
		// +----+----+-----+-----+-----+-----+-----+-----+-----+-----+
		System.out.println("Is busy>");
		pigpio.gpioWrite(registerSelectGpio, RS_INSTRUCTION);
		pigpio.gpioWrite(readWriteGpio, RW_READ);
		pigpio.gpioWrite(enableGpio, true);
		pigpio.gpioWrite(enableGpio, false);
		pigpio.gpioWrite(enableGpio, true);
		pigpio.gpioWrite(enableGpio, false);
		return pigpio.gpioRead(db7Gpio);
	} // End of isBusy
	
	private void writeRAM(byte value) throws PigpioException {
		System.out.println("Write RAM>");
		// +----+----+-----+-----+-----+-----+-----+-----+-----+-----+
		// | RS | RW | DB7 | DB6 | DB5 | DB4 | DB3 | DB2 | DB1 | DB0 |
		// +----+----+-----+-----+-----+-----+-----+-----+-----+-----+
		// | 0  | 1  |  BF | AC6 | AC5 | AC4 | AC3 | AC2 | AC1 | AC0 |
		// +----+----+-----+-----+-----+-----+-----+-----+-----+-----+
		write(RS_DATA, RW_WRITE, value);
	} // End of writeRAM

	private void write4bits(boolean registerSelect, boolean readWrite, byte value) throws PigpioException {
//		while(isBusy()) {
//			// Do nothing
//		}
		pigpio.gpioWrite(registerSelectGpio, registerSelect);
		pigpio.gpioWrite(readWriteGpio, readWrite);
		pigpio.gpioWrite(db7Gpio, Utils.isSet(value, 3));
		pigpio.gpioWrite(db6Gpio, Utils.isSet(value, 2));
		pigpio.gpioWrite(db5Gpio, Utils.isSet(value, 1));
		pigpio.gpioWrite(db4Gpio, Utils.isSet(value, 0));
		
		System.out.println("+----+----+----+----+----+----+");
		System.out.println(String.format("|  %s |  %s |  %s |  %s |  %s |  %s |", //
				Utils.bitString(registerSelect), //
				Utils.bitString(readWrite), //
				Utils.bitString(Utils.isSet(value, 3)), //
				Utils.bitString(Utils.isSet(value, 2)), //
				Utils.bitString(Utils.isSet(value, 1)), //
				Utils.bitString(Utils.isSet(value, 0))));
	} // End of write
	
	private void write8bits(boolean registerSelect, boolean readWrite, byte value) throws PigpioException {
//		while(isBusy()) {
//			// Do nothing
//		}
		pigpio.gpioWrite(registerSelectGpio, registerSelect);
		pigpio.gpioWrite(readWriteGpio, readWrite);
		pigpio.gpioWrite(db7Gpio, Utils.isSet(value, 7));
		pigpio.gpioWrite(db6Gpio, Utils.isSet(value, 6));
		pigpio.gpioWrite(db5Gpio, Utils.isSet(value, 5));
		pigpio.gpioWrite(db4Gpio, Utils.isSet(value, 4));
//		pigpio.gpioWrite(db3Gpio, Utils.isSet(value, 3));
//		pigpio.gpioWrite(db2Gpio, Utils.isSet(value, 2));
//		pigpio.gpioWrite(db1Gpio, Utils.isSet(value, 1));
//		pigpio.gpioWrite(db0Gpio, Utils.isSet(value, 0));

	} // End of write
	
	private void write(boolean registerSelect, boolean readWrite, byte value) throws PigpioException {
		System.out.println("Write >");
		if (!is8BitMode()) {
			write4bits(registerSelect, readWrite, (byte)(value >>> 4));
			pulseEnable();
			write4bits(registerSelect, readWrite, (byte)(value & 0b1111));
			pulseEnable();
		} else {
			write8bits(registerSelect, readWrite, value);
			pulseEnable();
		}
	} // End of write
		
		
	public void write(String text) throws PigpioException {
		byte data[] = text.getBytes();
		for (int i=0; i<data.length; i++) {
			writeRAM(data[i]);
		}
	} // End of write
	
	private void pulseEnable() throws PigpioException {
		pigpio.gpioWrite(enableGpio, false);
		pigpio.gpioDelay(100);
		pigpio.gpioWrite(enableGpio, true);
		pigpio.gpioDelay(100);
		pigpio.gpioWrite(enableGpio, false);
		pigpio.gpioDelay(100);
	} // End of pulseEnable
	
	private boolean is8BitMode() {
		return (functionMode & LCD_8BITMODE) != 0;
	} // End of is8BitMode
} // End of class
// End of file