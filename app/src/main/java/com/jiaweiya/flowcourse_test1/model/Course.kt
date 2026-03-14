package com.jiaweiya.flowcourse_test1.model

// 课程的数据定义
data class Course(
    val id: Int = 0,               // 课程的唯一编号
    val name: String,              // 课程名称
    val room: String,              // 上课教室
    val teacher: String,           // 授课老师
    val dayOfWeek: Int,            // 星期几上课
    val startNode: Int,            // 开始节数
    val endNode: Int,              // 结束节数
    val weekList: List<Int>        // 上课的周数; [1, 2, 3, 4, 5] 表示1到5周有课
)