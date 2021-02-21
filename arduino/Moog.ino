#include <HashMap.h>

#include "pitches.h"

const byte HASH_SIZE = 14;
//storage
HashType<char,int> hashRawArray[HASH_SIZE];
//handles the storage [search,retrieve,insert]
HashMap<char,int> keyMap = HashMap<char,int>( hashRawArray , HASH_SIZE );

// notes in the melody: 
int melody[] = {
  NOTE_C5, NOTE_B4, NOTE_A4, NOTE_G4, NOTE_F4, 0, NOTE_G4, NOTE_A4, NOTE_C5, NOTE_B4, NOTE_A4, NOTE_G4, NOTE_F4, 0, NOTE_B4, NOTE_A4, NOTE_G4, NOTE_F4, NOTE_E4, NOTE_F4, NOTE_G4, 0, NOTE_C5, NOTE_B4, NOTE_GS4, NOTE_G4, NOTE_E4, NOTE_C4
};

// note durations: 4 = quarter note, 8 = eighth note, etc.:
int noteDurations[] = {
  4, 8, 8, 4, 4, 4, 4, 4
};

void setup() {

  //setup hashmap
  keyMap[0]('c',NOTE_C4);   //c
  keyMap[1]('y',NOTE_CS4);  /c#
  keyMap[2]('d',NOTE_D4);   //d
  keyMap[3]('u',NOTE_DS4);  //d#
  keyMap[4]('e',NOTE_E4);   //e
  keyMap[5]('f',NOTE_F4);   //f
  keyMap[6]('i',NOTE_FS4);  //f#
  keyMap[7]('g',NOTE_G4);   //g
  keyMap[8]('o',NOTE_GS4);  //g#
  keyMap[9]('a',NOTE_A4);   //a
  keyMap[10]('p',NOTE_AS4); //a#
  keyMap[11]('b',NOTE_B4);  //b
  keyMap[12]('l',NOTE_C5);  //c5
  keyMap[13]('x',0);        //null
  
  Serial.begin(115200); //9600
  Serial.print("Ready");

  tone(8, NOTE_C4, 1000/4);

  //  playTone();
}

void playTone() {
  for (int thisNote = 0; thisNote < 20; thisNote++) {
    // to calculate the note duration, take one second divided by the note type.
    //e.g. quarter note = 1000 / 4, eighth note = 1000/8, etc.
    // int noteDuration = 1000 / noteDurations[thisNote];
    tone(8, melody[thisNote], 250);
    // to distinguish the notes, set a minimum time between them.
    // the note's duration + 30% seems to work well:
    int pauseBetweenNotes = 250 * 1.30;
    delay(pauseBetweenNotes);
    // stop the tone playing:
    noTone(8);
  }
}

char input = 'z';
int note = 0;
int oldNote = 0;
void loop() {
  if(Serial.available()) {
    input = Serial.read();
    note = keyMap.getValueOf(input);
  }
  if (oldNote != note) {
    oldNote = note;
  }
  if(note != 0) {
      tone(8, note, 1000/32);
  }
}
