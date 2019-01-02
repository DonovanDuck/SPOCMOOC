<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>课程详情页</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/course.css" type="text/css">
</head>
<body>
  <div class="header"></div>
      <input class="search" type="search" placeholder="请输入关键词"><button></button>
  <div class="banner">
      <div class="banner_t">
          <%-- <span>按系别分：</span>
          <ul>
          <c:forEach items="${ }" var>
          	<li><button>${category }</button></li>
          </c:forEach>
              
              
              
              <li><button>电子工程系</button></li>
              <li><button>自动化系</button></li>
              <li><button>化学与化工系</button></li>
              <li><button>计算机工程系</button></li>
              <li><button>环境与安全工程系</button></li>
              <li><button>材料工程系</button></li>
              <li><button>理学系</button></li>
              <li><button>经济与管理系</button></li>
              <li><button>外语系</button></li>
              <li><button>设计艺术系</button></li>
              <li><button>法学系</button></li>
              <li><button>体育系</button></li>
          </ul>
          <p style="font-size: 30px;"> > </p>
      </div>
      <div class="banner_m">
          <span>按年级分：</span>
          <ul>
              <li><button>大一</button> </li>
              <li><button>大二</button></li>
              <li><button>大三</button></li>
              <li><button>大四</button></li>
          </ul>
      </div> --%>
      <div class="banner_b">
          <span>课程：</span>
          <ul>
              <li><button>C++</button> </li>
              <li><button>java</button></li>
              <li><button>数据结构</button></li>
              <li><button>高等数学</button></li>
              <li><button>离散数学</button></li>
              <li><button>概率论</button></li>
              <li><button>英语</button></li>
              <li><button>马原</button></li>
              <li><button>思修</button></li>
              <li><button>毛概</button></li>
              <li><button>计导</button></li>
          </ul>
          <p style="font-size: 30px;"> > </p>
      </div>
  </div>
  <div class="main">
      <div class="item">全部课程：</div>
      <br><br>
      <div class="courses">
          <div class="course">
              <img src="../images/tu1.jpg" alt="">
              <p>概率论与数理统计</p>
          </div>
          <div class="course">
              <img src="../images/tu2.png" alt="">
              <p>数字图像处理</p>
          </div>
          <div class="course">
              <img src="../images/tu3.png" alt="">
              <p>面向对象分析与设计</p>
          </div>
          <div class="course">
              <img src="../images/tu4.png" alt="">
              <p>信息安全数学基础</p>
          </div>
          <div class="course">
              <img src="../images/tu5.png" alt="">
              <p>FLASH动画设计与制作</p>
          </div>
          <div class="course">
              <img src="../images/tu6.jpg" alt="">
              <p>数据结构</p>
          </div>
          <div class="course">
              <img src="../images/tu7.jpg" alt="">
              <p>大数据算法</p>
          </div>
          <div class="course">
              <img src="../images/tu8.jpg" alt="">
              <p>java程序设计</p>
          </div>
          <div class="course">
              <img src="../images/tu9.jpg" alt="">
              <p>C语言程序设计</p>
          </div>
          <div class="course">
              <img src="../images/tu10.jpg" alt="">
              <p>web编程技术</p>
          </div>
          <div class="course">
              <img src="../images/tu11.jpg" alt="">
              <p>计算机程序设计基础</p>
          </div>
          <div class="course">
              <img src="../images/tu12.jpg" alt="">
              <p>Python语言程序设计</p>
          </div>
      </div>
  </div>
  <div class="footer">
  </div>
</body>
</html>