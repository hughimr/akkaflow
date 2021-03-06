package com.kent.test

import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.io.ObjectInputStream
import java.io.IOException
import java.io.ByteArrayInputStream
import java.io.Serializable

class CloneTest extends Serializable{
  var a = 1;
  var b = new Child();
  def myclone:CloneTest = {
      var outer: CloneTest = null
      try { // 将该对象序列化成流,因为写在流里的是对象的一个拷贝，而原对象仍然存在于JVM里面。所以利用这个特性可以实现对象的深拷贝
          val baos = new ByteArrayOutputStream()
          val oos = new ObjectOutputStream(baos)
          oos.writeObject(this)
          // 将流序列化成对象
          val bais = new ByteArrayInputStream(baos.toByteArray())
          val ois = new ObjectInputStream(bais)
          outer = ois.readObject().asInstanceOf[CloneTest];
      } catch{
      case e:IOException => e.printStackTrace()
      case e:Exception => e.printStackTrace()
      }
      return outer;
  }
  class Child {
    var a = "aaaa"
  }
}

object CloneTest extends App{
  var c = new CloneTest;
  var c2 = c.myclone
  println(c2)
}