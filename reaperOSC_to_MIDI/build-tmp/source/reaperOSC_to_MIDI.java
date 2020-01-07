import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import oscP5.*; 
import netP5.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class reaperOSC_to_MIDI extends PApplet {

/*
  Processing example to connect via OSC 
  to Reaper Digital Audio Workstation
  using the midi virtual keyboard protocol
  Sonic Interaction Design
  Interaction Design
  ZHdK
  ************************
  ************************
  ************************

  January 2020
  By ndr3s -v -t (Andrés Villa Torres)
*/



OscP5 oscP5;
NetAddress myRemoteLocation;
midiKEYS [] keys = new midiKEYS[22];
midiWHEEL wheel ;
public void setup() {
  
  frameRate(25);
  oscP5 = new OscP5(this, 12000);
  // 127.0.0.1 stands for local device, you can replace this with the ip address of 
  // another computer in your local network. Be sure that your network configuration allows UDP traffic
  // ZHdK blocks UDP communication therefore we use a router
  myRemoteLocation = new NetAddress("127.0.0.1", 11000);
  for (int i = 0; i < keys.length; i ++) {
    // class constructor ::> midiKEYS(float _x, float _y, int _id, int _shift)
    keys[i] = new midiKEYS( 50 + (i * 40), 25, i, 12);
  }
  wheel = new midiWHEEL(300,300, 125);
}
public void draw() {
  background(0); 
  for (int i = 0; i < keys.length; i ++) {
    keys[i].update();
    keys[i].display();
  }
  wheel.update();
  wheel.display();
}
  
public void mouseWheel(MouseEvent event){
  float e = event.getCount();
  wheel.updateScroll(e);
}
//midi wheel class
class midiWHEEL{

  float x, y;
  int amount=18;
  boolean [] kPlayed=new boolean[amount];
  PVector pos;
  int id;
  int shift;
  float angle=0;
  float size=200;
  float smoothSize=200;
  PVector tip;
  float r;
  boolean engaged=false;
  PVector [] contactPoints = new PVector [amount];
  midiWHEEL(float _x, float _y, float _r) {
    x = _x; 
    y = _y;
    r = _r;
    pos = new PVector(x, y);
    tip = new PVector(x+r,y);
    for(int i= 0; i < contactPoints.length; i ++){
          contactPoints[i] = new PVector(
            pos.x + r*cos(radians( (360/contactPoints.length)*i)),
            pos.y + r*sin(radians((360/contactPoints.length)*i))
            );   
            kPlayed[i]=false;   
    }

  }


  public void update(){
    PVector person = new PVector(mouseX, mouseY);

    if(person.dist(pos)< 100){
      engaged=true;
      tip.x = pos.x + r*cos(radians(angle));
      tip.y = pos.y + r*sin(radians(angle));
      size=300;
      soundOnOff();
    }else{
      engaged=false;
      size=200;
    }
    smoothSize= smoothSize*0.5f + size*0.5f;
  }

  public void soundOnOff(){
    for(int i= 0; i < contactPoints.length; i ++){
      if(tip.dist(contactPoints[i])<10 && !kPlayed[i]){
          println( "contact point id : " + i  + " engaged ");
          noteON(20+i);
          kPlayed[i]=true;

      
      }else{
        if (kPlayed[i] && tip.dist(contactPoints[i])>12 ) {
          println( "contact point id : " + i  + " disarmed ");
          noteOFF(20+i);
          kPlayed[i]=false;
     
        }
      }
    }
  }
  public void noteON(int _note){
    OscMessage myMessage = new OscMessage("i/vkb_midi/0/note/"+_note);
    myMessage.add(50.0f); 
    oscP5.send(myMessage, myRemoteLocation);
  }
  public void noteOFF(int _note){
    OscMessage myMessage = new OscMessage("i/vkb_midi/0/note/"+_note);
    myMessage.add(0); 
    oscP5.send(myMessage, myRemoteLocation);
  }
  public void updateScroll(float e){
      if(engaged){
        angle = angle + (e*1);
      }
  }
  public void display(){
    noFill();
    strokeWeight(2);
    stroke(255,0,0);
    rectMode(CENTER);
    ellipse(pos.x,pos.y,smoothSize,smoothSize);
    ellipse(tip.x,tip.y,20,20);
    fill(255,0,0);
    // text(tip.x + ", "+ tip.y, tip.x+15,tip.y+15);
    
    fill(255,0,0);
    for(int i= 0; i < contactPoints.length; i ++){

      ellipse(contactPoints[i].x, contactPoints[i].y, 10, 10);
    }
  }
}

// midi keys class
class midiKEYS {
  float x, y;
  boolean kPlayed=false;
  PVector pos;
  int id;
  int shift;
  midiKEYS(float _x, float _y, int _id, int _shift) {
    id = _id;
    x = _x; 
    y = _y;
    pos = new PVector(x, y);
    shift = _shift;
  }
  public void update() {
    PVector person = new PVector(mouseX, mouseY);

    if (mousePressed && !kPlayed && abs(person.x - (pos.x) )<20 && person.y>25 && person.y < 105 ) {
      println( "key id : " + id  + " pressed ");
      kPlayed = true;
      noteON_MIDIviaOSC(shift);
    } else {
      if (kPlayed && !mousePressed ) {
        println( "key id : " + id  + " un pressed ");
        kPlayed=false;
        noteOFF_MIDIviaOSC(shift);
      }
    }
  }
  public void display() {
    PVector person = new PVector(mouseX, mouseY);
    noStroke();
    if (abs(person.x - (pos.x) )<20 && person.y>25 && person.y < 105) {
      fill(255, 200, 200);
    } else {
      fill(255, 0, 0);
    }
    rectMode(CORNER);
    rect(x-19, y, 38, 80);
    text(id , x-19, y + 95);
  }
  public void noteON_MIDIviaOSC(int _shift) {
    int tNote = id + _shift;
    //  the osc tag that is received and understood by reaper
    //  addressing the midi virtual keyboard > "i/vkb_midi/0/note/@"
    OscMessage myMessage = new OscMessage("i/vkb_midi/0/note/"+tNote);
    // adding the value "50.0" means 50% of velocity (speed or pressure)
    // if increased this will cause the note to be louder
    // if the value added is "0" this sets the note to OFF
    myMessage.add(50.0f); 
    oscP5.send(myMessage, myRemoteLocation);
  }
  public void noteOFF_MIDIviaOSC(int _shift) {
    int tNote = id + _shift;
    OscMessage myMessage = new OscMessage("i/vkb_midi/0/note/"+tNote);
    // the value added is "0" and this sets the note to OFF
    myMessage.add(0);
    oscP5.send(myMessage, myRemoteLocation);
  }
}



  public void settings() {  size(950, 600, FX2D); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "--present", "--window-color=#666666", "--stop-color=#cccccc", "reaperOSC_to_MIDI" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
