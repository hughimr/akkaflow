package com.kent.workflow.actionnode

import com.kent.workflow.node.ActionNodeInstance
import com.kent.workflow.node.NodeInstance
import scala.sys.process.ProcessLogger
import com.kent.pub.ShareData
import com.kent.db.LogRecorder._
import com.kent.coordinate.ParamHandler
import scala.sys.process._
import java.util.Date
import java.io.PrintWriter
import java.io.File
import com.kent.util.Util

class ScriptActionNodeInstance(override val nodeInfo: ScriptActionNodeInfo) extends ActionNodeInstance(nodeInfo)  {
  var executeResult: Process = _
  def deepClone(): ScriptActionNodeInstance = {
    val sani = ScriptActionNodeInstance(nodeInfo)
    deepCloneAssist(sani)
    sani
  }
  def deepCloneAssist(hsni: ScriptActionNodeInstance): ScriptActionNodeInstance = {
    super.deepCloneAssist(hsni)
    hsni.executeResult = executeResult
    hsni
  }

  override def execute(): Boolean = {
    try {
      var newLocation = if(nodeInfo.location == null || nodeInfo.location == "")
            ShareData.config.getString("workflow.action.script-location") + "/" + Util.produce8UUID
            else nodeInfo.location
      //把文件内容写入文件
      val writer = new PrintWriter(new File(newLocation))
      //删除前置空格
      val lines = nodeInfo.content.split("\n").map { x => 
        if(x.trim() != "")x.trim() else null
      }.filter { _ != null }
      val trimContent = lines.mkString("\n")
      writer.write(trimContent)
      writer.flush()
      writer.close()
      ShareData.logRecorder ! Info("NodeInstance", this.id, s"代码写入到文件：${newLocation}")
      //执行
      var cmd = "sh";
      if(lines.size > 0){  //选择用哪个执行解析器
        if(lines(0).toLowerCase().contains("perl")) cmd = "perl"
        else if(lines(0).toLowerCase().contains("python")) cmd = "python"
      }
      
      val pLogger = ProcessLogger(line => ShareData.logRecorder ! Info("NodeInstance", this.id, line),
                                  line => ShareData.logRecorder ! Error("NodeInstance", this.id, line))
       executeResult = Process(s"${cmd} ${newLocation}").run(pLogger)
       if(executeResult.exitValue() == 0) true else false
    }catch{
      case e:Exception => ShareData.logRecorder ! Error("NodeInstance", this.id, e.getMessage);false
    }
  }

  def replaceParam(param: Map[String, String]): Boolean = {
    nodeInfo.location = ParamHandler(new Date()).getValue(nodeInfo.location, param)
    nodeInfo.content = ParamHandler(new Date()).getValue(nodeInfo.content, param)
    true
  }

  def kill(): Boolean = {
    executeResult.destroy()
    true
  }
}

object ScriptActionNodeInstance {
  def apply(san: ScriptActionNodeInfo): ScriptActionNodeInstance = {
    val sa = san.deepClone() 
    val sani = new ScriptActionNodeInstance(sa)
    sani
  }
}