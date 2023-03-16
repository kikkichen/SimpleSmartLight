#include "SCoop.h"
#include <SoftwareSerial.h>
#define Sortware_TX 8
#define Sortware_RX 9

byte read_data, bt_data;
SoftwareSerial BT_model(Sortware_RX, Sortware_TX);

int ANIMDELAY = 120;  // animation delay, deafault value is 100
int INTENSITYMIN = 0; // minimum brightness, valid range [0,15]
int INTENSITYMAX = 8; // maximum brightness, valid range [0,15]
 
int DIN_PIN = 12;      // data in pin
int CS_PIN = 10;       // load (CS) pin
int CLK_PIN = 11;      // clock pin

// Sensor
int LedBlue = 3;
int LedRed = 4;
int LedGreen = 5;
int LedYellow = 6;
int LedWhite = 7;
int Sensor = 2;

// global value
int boot_sign = 0;  // ????
int animation_switch = 0;   // ????????
int which_light_is_lighting = LedWhite;   // ??????
int setTime = 0; // ????
 
// MAX7219 registers
byte MAXREG_DECODEMODE = 0x09;
byte MAXREG_INTENSITY  = 0x0a;
byte MAXREG_SCANLIMIT  = 0x0b;
byte MAXREG_SHUTDOWN   = 0x0c;
byte MAXREG_DISPTEST   = 0x0f;
 
const unsigned char heart[] = {
  B01100110,
  B11111111,
  B11111111,
  B11111111,
  B01111110,
  B00111100,
  B00011000,
  B00000000
};

const unsigned char Square[] = {
  B11111111,
  B11000011,
  B10100101,
  B10011001,
  B10011001,
  B10100101,
  B11000011,
  B11111111
};
 
const unsigned char star[] = {
  B00001000,
  B00011000,
  B01111110,
  B00111100,
  B00111000,
  B00101100,
  B01000010,
  B00000000
};

const unsigned char triple[] = {
  B00001000,
  B00011000,
  B00111100,
  B11111110,
  B01111111,
  B00111100,
  B00011000,
  B00010000,
};

const unsigned char monster[] = {
  B00100100,
  B01100110,
  B01111110,
  B10100101,
  B10100101,
  B10000001,
  B01011010,
  B00100100,
};

const unsigned char ghost[] = {
  B00111100,
  B01000010,
  B10100101,
  B10000001,
  B10111101,
  B10000001,
  B01010101,
  B00101010,
};

const unsigned char none[] = {
  B00000000,
  B00000000,
  B00000000,
  B00000000,
  B00000000,
  B00000000,
  B00000000,
	B00000000,
};

defineTask(TaskBT);
defineTask(TaskRunTouchSensor);
defineTask(TaskOnLight);
defineTask(TaskAnimationOnLED);
defineTask(TaskSetClock);

  B00000000
};

defineTask(TaskBT);
defineTask(TaskRunTouchSensor);
defineTask(TaskOnLight);
defineTask(TaskAnimationOnLED);
defineTask(TaskSetClock);

void setup () {
  mySCoop.start();
}
 
void loop () {
  yield();
}

// ????
void TaskBT::setup() {
  Serial.begin(9600);
  BT_model.begin(9600);
}

void TaskBT::loop() {
  if (BT_model.available()) {
    bt_data = BT_model.read();
    delay(2);
    int com = bt_data;
    
    if (boot_sign == 0 && com == 0x01) {
      // ??
      boot_sign = 1;
    } else if (boot_sign == 1 && com == 0x00) {
      // ??
      Serial.println(bt_data);
      boot_sign = 0;
      changeAnimation(0x00);
    } else if (boot_sign == 1) {
      // ????
      if (bt_data >= 0x90 && bt_data <= 0x94) {
        if (bt_data == 0x91) {setTime = 15;}
        else if (bt_data == 0x92) {setTime = 30;}
        else if (bt_data == 0x93) {setTime = 105;}
        else if (bt_data == 0x94) {setTime = 130;}
        else {setTime = 0;}
        tone(13, 4000, 200);
      } else if (bt_data < 0x08 || bt_data > 0x15) {
        which_light_is_lighting = bt_data; // ????? ??
      } else { // ????
        if (bt_data == 0x14) {
          animation_switch = 1; 
          changeAnimation(0x08);
        } else if (bt_data == 0x15) {
          animation_switch = 0; 
          changeAnimation(0x00);
        }else {
          changeAnimation(bt_data);
        }
      }
    } else {
      boot_sign = 0;
    }
  }
  if (Serial.available() > 0) {
    read_data = Serial.read();
    delay(2);
    BT_model.write(read_data);
  }
}

// ????
void TaskSetClock::setup() {
  setTime = 0;
}

void TaskSetClock::loop() {
  if (boot_sign > 0 && setTime > 0) {
    if (setTime <= 1) { boot_sign = 0; }
    setTime = setTime - 1;
    delay(1000);
  } else {
    
  }  
}

// ??????
void TaskRunTouchSensor::setup() {
  initTouchSenor();
}

void TaskRunTouchSensor::loop() {
  loopTouchSenor();
}

// LED???
void TaskOnLight::setup() {
  initLedLight();
}

void TaskOnLight::loop() {
  if (boot_sign == 1) {
    switch(which_light_is_lighting) {
      case 2: {
        // ???
        for (int i=3; i<=7; i++) {
          ShokeLight(i, true);
          delay(100);
        }
        break;
      }
      case 3 : {
        // ??
        ShokeLight(LedBlue, true); break;
      }
      case 4 : {
        // ??
        ShokeLight(LedRed, true); break;
      }
      case 5 : {
        // ??
        ShokeLight(LedGreen, true); break;
      }
      case 6 : {
        // ??
        ShokeLight(LedYellow, true); break;
      }
      // ????
      default : {
        ShokeLight(LedWhite, true); 
        break;
      }
    }
  } else {
    ShokeLight(1, false);
  }
}

// 8x8 ????
void TaskAnimationOnLED::setup() {
  initSetupDraw();  
}

void TaskAnimationOnLED::loop() {
  loopAnimation();
}

void setRegistry(byte reg, byte value) {
  digitalWrite(CS_PIN, LOW);
 
  putByte(reg);   // specify register
  putByte(value); // send data
 
  digitalWrite(CS_PIN, LOW);
  digitalWrite(CS_PIN, HIGH);
}
 
void putByte(byte data) {
  byte i = 8;
  byte mask;
  while (i > 0)
  {
    mask = 0x01 << (i - 1);        // get bitmask
    digitalWrite( CLK_PIN, LOW);   // tick
    if (data & mask)               // choose bit
      digitalWrite(DIN_PIN, HIGH); // send 1
    else
      digitalWrite(DIN_PIN, LOW);  // send 0
    digitalWrite(CLK_PIN, HIGH);   // tock
    --i;                           // move to lesser bit
  }
}

void initSetupDraw() {
  pinMode(DIN_PIN, OUTPUT);
  pinMode(CLK_PIN, OUTPUT);
  pinMode(CS_PIN, OUTPUT);
 
  // initialization of the MAX7219
  setRegistry(MAXREG_SCANLIMIT, 0x07);
  setRegistry(MAXREG_DECODEMODE, 0x00);  // using an led matrix (not digits)
  setRegistry(MAXREG_SHUTDOWN, 0x01);    // not in shutdown mode
  setRegistry(MAXREG_DISPTEST, 0x00);    // no display test
  setRegistry(MAXREG_INTENSITY, 0x0f & INTENSITYMIN);
 
  // draw hearth
  changeAnimation(0x00);
}

void loopAnimation() {
    // second beat
  setRegistry(MAXREG_INTENSITY, 0x0f & INTENSITYMAX);
  delay(ANIMDELAY);
  
  // switch off
  setRegistry(MAXREG_INTENSITY, 0x0f & INTENSITYMIN);
  delay(ANIMDELAY);
  
  // second beat
  setRegistry(MAXREG_INTENSITY, 0x0f & INTENSITYMAX);
  delay(ANIMDELAY);
  
  // switch off
  setRegistry(MAXREG_INTENSITY, 0x0f & INTENSITYMIN);
  delay(ANIMDELAY*6);
}

// 8x8 ??????
void changeAnimation(byte led) {
  // ??????????????
    if (animation_switch != 0) {
      if (led == 0x08) {
      for (int i = 1; i<=8; i++) {
          setRegistry(i, monster[i-1]);
        }
    } else if (led == 0x09) {
      for (int i = 1; i<=8; i++) {
          setRegistry(i, Square[i-1]);
        }
    } else if (led == 0x10) {
      for (int i = 1; i<=8; i++) {
          setRegistry(i, heart[i-1]);
        }
    } else if ( led == 0x11) {
      for (int i = 1; i<=8; i++) {
          setRegistry(i, ghost[i-1]);
        }
    } else if ( led == 0x12) {
      for (int i = 1; i<=8; i++) {
          setRegistry(i, triple[i-1]);
        }
    } else if ( led == 0x13) {
      for (int i = 1; i<=8; i++) {
          setRegistry(i, star[i-1]);
        }
    } else {
      for (int i = 1; i<=8; i++) {
          setRegistry(i, none[i-1]);
        }
    }
  } else {
    for (int i = 1; i<=8; i++) {
      setRegistry(i, none[i-1]);
    }
  }
}

void initTouchSenor() {
  pinMode(Sensor, INPUT);
  Serial.begin(9600);
}

void loopTouchSenor() {
  boot_sign = boot_sign ^ digitalRead(Sensor);
  if (boot_sign == 0) {
    changeAnimation(0x00);
  }
  Serial.println(boot_sign);
}

// ?????LED?
void initLedLight() {
  pinMode(LedBlue, OUTPUT);
  pinMode(LedRed, OUTPUT);
  pinMode(LedGreen, OUTPUT);
  pinMode(LedYellow, OUTPUT);
  pinMode(LedWhite, OUTPUT);
}

void ShokeLight(int light, bool open) {
  for (int i = 3; i<=7; i++) {
    if (light == i && open) {
      digitalWrite(i, HIGH);
    } else {
      digitalWrite(i, LOW);
    }
  }
}