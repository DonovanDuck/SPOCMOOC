package cn.edu.tit.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSONObject;

import cn.edu.tit.bean.Accessory;
import cn.edu.tit.bean.Category;
import cn.edu.tit.bean.Course;
import cn.edu.tit.bean.RealClass;
import cn.edu.tit.bean.Resource;
import cn.edu.tit.bean.ResourceType;
import cn.edu.tit.bean.Student;
import cn.edu.tit.bean.Task;
import cn.edu.tit.bean.Teacher;
import cn.edu.tit.bean.Term;
import cn.edu.tit.bean.VirtualClass;
import cn.edu.tit.common.Common;
import cn.edu.tit.iservice.IAdminService;
import cn.edu.tit.iservice.IResourceService;
import cn.edu.tit.iservice.IStudentService;
import cn.edu.tit.iservice.ITeacherService;
import net.sf.json.JSONArray;

@RequestMapping("/teacher")
@Controller
public class TeacherController {
	/**
	 * 声明变量
	 * */
	@Autowired
	private ITeacherService teacherService;
	@Autowired
	private IStudentService studentService;
	@Autowired
	public MainController mainController;
	@Autowired
	private IResourceService resourceService;
	private static List<Category> categories = null;//将  分类 信息作为全局变量，避免多次定义,在首次登陆教师页面时 在  方法teacherCourseList（） 处即初始化成功

	@RequestMapping(value="teacherLogin",method= {RequestMethod.GET})
	public ModelAndView teacherLogin( @RequestParam("employeeNum")String teacherId,@RequestParam("password")String password,HttpServletRequest request) {
		ModelAndView mv = new ModelAndView();
		String readResult =null;
		request.getSession().setAttribute("teacherId", null);
		String teacherPassword = null;
		try {
			Teacher teacher = teacherService.teacherLoginByEmployeeNum(teacherId);
			teacherPassword = Common.eccryptMD5(password);
			if(teacherPassword.equals(teacher.getTeacherPassword()) )
			{	
				request.getSession().setAttribute("teacherId", teacher.getEmployeeNum());
				request.getSession().setAttribute("teacher", teacher);
				mv=mainController.toMain(request); //去首页
				mv.addObject("readResult", "登录成功");//返回信息
				mv.addObject("teacher",teacher);
			}
			else {
				mv.addObject("readResult", "密码错误");//返回信息
				mv.setViewName("/jsp/Teacher/index");//设置返回页面
			}
		} catch (Exception e) {
			mv.addObject("readResult", "异常");//返回信息
			mv.setViewName("/jsp/Teacher/index");//设置返回页面
			e.printStackTrace();
		}
		return mv;	
	}

	/**
	 * @author LiMing
	 * @param request
	 * @return
	 * @throws Exception 
	 * 课程二级页面
	 */
	@RequestMapping(value="courseList",method= {RequestMethod.GET})
	public ModelAndView toCourseSecond(HttpServletRequest request) throws Exception {
		ModelAndView mv = new ModelAndView();
		categories = teacherService.readCategory();
		List<Course> list = new ArrayList<Course>();
		List<String> teacherNames = new ArrayList<String>();
		list = teacherService.readCourse(null);
		for (Course course : list) 
		{
			System.out.println(course.toString());
			teacherNames.add(teacherService.getTeacherNameById(course.getPublisherId()));
		}
		mv.addObject("categories", categories);
		mv.addObject("courseList", list);
		mv.addObject("teacherNames", teacherNames);
		mv.setViewName("/jsp/CourseJsp/courseSecond");
		return mv;
	}
	/**
	 * 跳转到教师的课程详细页面
	 * @param request
	 * @return
	 */
	@RequestMapping(value="toCourseDetail/{courseId}")
	public String toCourseDetail(HttpServletRequest request, @PathVariable String courseId){
		// 通过courseid查询课程
		Course course = null;
		try {
			course = teacherService.getCourseById(courseId);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// 查询教师圈教师信息
		List<Teacher> teacherList = teacherService.getTeachersByCourseId(courseId);
		request.getSession().setAttribute("teacherList", teacherList); //通过存入request在前台访问
		//request.getSession().setAttribute("course", course);
		// 查出操作者是否是manager
		Teacher teacher = (Teacher) request.getSession().getAttribute("teacher");
		if(teacher != null){
			//查manager
			Integer manager = teacherService.getManagerByEmployeeNum(teacher.getEmployeeNum(), courseId);
			request.setAttribute("manager", manager);
		}
		//查操作者是否是学生
		Student student = (Student) request.getSession().getAttribute("student");
		if(student != null){
			//查出所在虚拟班级信息
			VirtualClass virtualClass = teacherService.getVirtualClassByRidAndCid(student.getClassNum(), courseId);
			request.setAttribute("virtualClass", virtualClass);
		}
		request.setAttribute("student", student);
		//查询课程类别名
		String category = teacherService.getCategoryById(course.getCourseCategory());
		request.setAttribute("category", category);
		request.getSession().setAttribute("course", course);
		return "jsp/Teacher/course_detail";
	}

	/**
	 * 跳转到创建课程界面
	 * @param request
	 * @param employeeNum
	 * @return
	 */
	@RequestMapping("toCreateCourse")
	public String toCreateCourse(HttpServletRequest request){
		try {
			//查找所有系部列表
			List<Category> categoryList =  teacherService.readCategory();
			Teacher teacher = (Teacher) request.getSession().getAttribute("teacher");
			Student student = (Student) request.getSession().getAttribute("student");
			request.setAttribute("teacher", teacher);
			request.setAttribute("student", student);
			request.getSession().setAttribute("categoryList", categoryList);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return "jsp/Teacher/createCourse";
	}

	/**
	 * 通过ajax获取教师列表
	 */
	@RequestMapping(value="ajaxGetTeachers")
	public void ajaxGetTeachers(HttpServletRequest request, HttpServletResponse response){
		try {
			List<Teacher> teacherList = new ArrayList<>();
			Teacher teach = (Teacher) request.getSession().getAttribute("teacher");
			String employeeNum = teach.getEmployeeNum();
			for(Teacher teacher : teacherService.getTeachers()){
				if(!employeeNum.equals(teacher.getEmployeeNum())){ // 在选择的教师中过滤掉当前的操作者
					teacherList.add(teacher);
				}
			}
			JSONArray  json  =  JSONArray.fromObject(teacherList); 
			String result = json.toString();
			response.getWriter().print(result);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}

	/**
	 * @author LiMing
	 * 通过ajax获取班级列表
	 */
	@RequestMapping(value="ajaxGetRealClass")
	public void ajaxGetRealClass(HttpServletRequest request, HttpServletResponse response){
		try {
			List<RealClass> realClassList = new ArrayList<RealClass>();
			realClassList = teacherService.readRealClass(null);
			for (RealClass realClass : realClassList) {
				System.out.println(realClass.toString());
			}
			JSONArray  json  =  JSONArray.fromObject(realClassList); //将获取的List集合存入 JSONArray中
			String result = json.toString();
			response.getWriter().print(result);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 用户关注课程
	 * @param request
	 * @param response
	 */
	@RequestMapping(value="ajaxAttentionCourse")
	public void ajaxAttentionCourse(HttpServletRequest request, HttpServletResponse response){
		String result = "";
		try {
			request.setCharacterEncoding("utf-8");
			response.setContentType("text/html;charset=UTF-8");
			//,@RequestParam(value="courseId")String courseId
			String courseId = request.getParameter("courseId");
			//判断是否登录
			Teacher teacher = (Teacher) request.getSession().getAttribute("teacher");
			Student student = (Student) request.getSession().getAttribute("student");
			if(teacher == null && student == null){
				result = JSONObject.toJSONString("请先登录");
			}
			//如果登录，则关注课程
			if(teacher != null){
				teacherService.teacherAttentionCourse(courseId, teacher.getEmployeeNum()); 
				result = JSONObject.toJSONString("关注成功！");
			}
			else if(student != null){
				studentService.studentAttentionCourse(courseId, student.getStudentId());
				result = JSONObject.toJSONString("关注成功！");
			}
			response.getWriter().println(result);

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			result = JSONObject.toJSONString("请不要重复关注！");
		}
		try {
			response.getWriter().println(result);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * @author LiMing
	 * 创建虚拟班级
	 * @throws Exception 
	 */
	@SuppressWarnings("unused")
	@RequestMapping(value="createVirtualClass",method = RequestMethod.POST)
	public String createVirtualClass(HttpServletRequest request,@RequestParam("className")String className,@RequestParam("selectTerm")String selectTerm,@RequestParam("realClassContent")String realClassContent) throws Exception{	
		Course course = (Course) request.getSession().getAttribute("virtualCourse");
		String courseId = course.getCourseId();
		String courseName = course.getCourseName();
		VirtualClass vir = new VirtualClass();
		try {
			//设置对象的属性
			vir.setCourseId(courseId);
			String uuid = Common.uuid();
			Timestamp publishTime = new Timestamp(System.currentTimeMillis());
			vir.setCreateTime(publishTime);
			Teacher teacher = (Teacher) request.getSession().getAttribute("teacher");
			vir.setCreatorId(teacher.getEmployeeNum());
			vir.setFaceImg(course.getFaceImg());
			vir.setVirtualClassNum(uuid);
			vir.setTerm(selectTerm);
			vir.setVirtualCourseName(course.getCourseName());
			//将前台得到的字符串分割
			String[] sourceStrArray = realClassContent.split(",");
			List<String> realClassArray = new ArrayList<String>();
			//字符串数组判空
			for(int i = 0;i<sourceStrArray.length;i++) {
				if(!sourceStrArray[i].isEmpty())
				{
					realClassArray.add(sourceStrArray[i]);
				}
			}
			List<RealClass> realClassList = new ArrayList<RealClass>();
			int count = 0;//班级总人数
			//将所有班级号转化为对应对象
			for (String string : realClassArray) {
				realClassList.add(teacherService.readRealClass(string).get(0));//查询出的始终只有一个
			}
			//计算总人数
			for (RealClass realClass : realClassList) {
				count+= realClass.getRealPersonNum();
			}
			vir.setVirtualClassName(className);
			vir.setClassStuentNum(count);
			vir.setRealClassList(realClassList);
			teacherService.createVirtualClass(vir);
			/**********实体班和虚拟班的对应***************/
			for (int i = 0; i < realClassArray.size(); i++) {
				teacherService.mapVirtualRealClass(realClassArray.get(i),uuid);
			}
			return toCourseDetail(request,courseId);//创建虚拟班级成功返回到课程三级页面
		} catch (Exception e) {
			e.printStackTrace();
			return toCourseDetail(request,courseId);//创建虚拟班级成功返回到课程三级页面
		} 
	}

	/**
	 * @author LiMing
	 * @param request
	 * @return
	 * 跳转到创建虚拟班级
	 * @throws Exception 
	 */
	@RequestMapping(value="toCreateVirtualClass/{courseId}")
	public ModelAndView toCreateVirtualClass(@PathVariable String courseId,HttpServletRequest request) throws Exception {
		ModelAndView mv = new ModelAndView();
		List<Term> listTerm = new ArrayList<Term>();
		List<RealClass> listRealClass = new ArrayList<RealClass>();

		Course course = new Course();
		course = teacherService.readCourseByCourseId(courseId);
		listTerm = teacherService.readTerm();
		listRealClass = teacherService.readRealClass(null);
		request.getSession().setAttribute("virtualCourse", course);//将course放入SESSION
		mv.addObject("course",course);
		mv.addObject("listTerm",listTerm);
		mv.addObject("listRealClass", listRealClass);
		mv.setViewName("/jsp/CourseJsp/createVirtualClass");
		return mv;
	}
	/**
	 * @author wenli
	 * @param request
	 * @return
	 * 去编辑虚拟班级
	 */
	@RequestMapping(value="toEditVirtualClass")
	public ModelAndView toEditVirtualClass(HttpServletRequest request) {


		ModelAndView mv = new ModelAndView();
		List<Term> listTerm = new ArrayList<Term>();
		List<RealClass> listRealClass = new ArrayList<RealClass>();
		String virtualClassNum = (String) request.getSession().getAttribute("virtualClassNum");
		VirtualClass virtualClass = teacherService.getVirtualById(virtualClassNum);
		String virtualClassName = (String) request.getSession().getAttribute("virtualClassName");
		String creator = (String) request.getSession().getAttribute("teacherId");
		Course course = (Course) request.getSession().getAttribute("course");
		try {
			listTerm = teacherService.readTerm();
			listRealClass = teacherService.getRealClassList(virtualClassNum);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		mv.addObject("listTerm",listTerm);
		mv.addObject("listRealClass", listRealClass);
		mv.addObject("virtualClassNum", virtualClassNum);
		mv.addObject("virtualClassName", virtualClassName);
		mv.addObject("creator", creator);
		mv.addObject("course", course);
		mv.setViewName("/jsp/Teacher/editVirtualClass");
		return mv;
	}

	/**
	 * 跳转到课程简介详细模块
	 * @param request
	 * @param courseId
	 * @return
	 */
	@RequestMapping(value="toCourseIntroduce/{courseId}")
	public String toCourseIntroduce(HttpServletRequest request, @PathVariable String courseId){
		try {
			//根据id查询课程
			Course course = teacherService.getCourseById(courseId);
			request.setAttribute("courseDetail", course.getCourseDetail());

		} catch (Exception e) {
			e.printStackTrace();
		}
		return "jsp/Teacher/lesson-introduce";
	}
	/**
	 * 创建课程
	 * @return
	 */
	@RequestMapping(value="createCourse")
	@SuppressWarnings({ "unused", "unchecked" })
	public ModelAndView createCourse(HttpServletRequest request){
		try {
			String courseId = Common.uuid();
			Object[] obj = Common.fileFactory(request,courseId);
			List<File> files = (List<File>) obj[0];	// 获取课程图片
			Map<String, Object> formdata = (Map<String, Object>) obj[1]; // 获取课程内容
			//封装课程类
			Course course = new Course();
			course.setCourseId(courseId);
			course.setCourseName((String)formdata.get("courseName"));
			course.setCourseDetail((String)formdata.get("courseDetail"));
			course.setCourseCategory((String)formdata.get("courseCategory"));
			Timestamp publishTime = new Timestamp(System.currentTimeMillis());
			course.setPublishTime(publishTime);
			String employeeNum = (String)formdata.get("publisherId");
			String teacherStr = (String)formdata.get("teacherContent");
			String[] teachers = teacherStr.split(",");
			course.setPublisherId(employeeNum);
			for(File f : files){ // 集合中只有一张图片
				course.setFaceImg(Common.readProperties("path")+"/"+f.getName());
			}
			teacherService.createCourse(course); // 添加课程
			teacherService.addOtherToMyCourse(employeeNum, courseId, 1);//把课程创建者初始化到教师圈
			//通过课程id和获取教师圈的id集合绑定教师到课程
			if(teachers != null){
				for(int i = 0; i < teachers.length; i++){
					teacherService.addOtherToMyCourse(teachers[i], courseId, 0);
				}
			}
			return toCourseSecond(request);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			return null;
		}

	}



	/**
	 * 修改课程
	 * @return
	 */
	@RequestMapping(value="modifyCourse/{courseId}")
	@SuppressWarnings({ "unused", "unchecked" })
	public String modifyCourse(HttpServletRequest request,@PathVariable String courseId){
		try {
			Object[] obj = Common.fileFactory(request,courseId);
			List<File> files = (List<File>) obj[0];	// 获取课程图片
			Map<String, Object> formdata = (Map<String, Object>) obj[1]; // 获取课程内容
			//封装课程类
			Course course = new Course();
			course.setCourseId(courseId);
			course.setCourseName((String)formdata.get("courseName"));
			course.setCourseDetail((String)formdata.get("courseDetail"));
			course.setCourseCategory((String)formdata.get("courseCategory"));
			Timestamp publishTime = new Timestamp(System.currentTimeMillis());
			course.setPublishTime(publishTime);
			String employeeNum = (String)formdata.get("publisherId");
			course.setPublisherId(employeeNum);
			if(!files.isEmpty()){
				for(File f : files){
					course.setFaceImg(Common.readProperties("path")+"/"+f.getName());
				}
			}else{
				//如果课程图片未被修改则保留原始路径
				String path = teacherService.getImgpathByCourseId(courseId);
				if(path.isEmpty()){
					path = Common.readProperties("path")+"/1.png";
				}
				course.setFaceImg(path);
			}
			teacherService.updateCourse(course); // 修改课程
			/*teacherService.addOtherToMyCourse(employeeNum, courseId, 1);//把课程创建者初始化到教师圈
			//通过课程id和获取教师圈的id集合绑定教师到课程
			if(teachers != null){
				for(int i = 0; i < teachers.length; i++){
					teacherService.addOtherToMyCourse(teachers[i], courseId, 0);
				}
			}*/
			return toCourseDetail( request,courseId);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			return null;
		}

	}

	/**
	 * 跳转到修改课程页面
	 * @param request
	 * @param courseId
	 * @return
	 */
	@RequestMapping(value="toModifyCourse/{courseId}")
	public ModelAndView toModifyCourse(HttpServletRequest request, @PathVariable String courseId){
		ModelAndView mv = new ModelAndView();
		try {
			// 查出课程信息
			Course course = teacherService.getCourseById(courseId);
			if(course != null){
				Teacher teacher = (Teacher) request.getSession().getAttribute("teacher"); //从session中获取教师工号
				if(teacher == null){
					mv.addObject("readResult", "请先登录");//返回信息
					mv.setViewName("/jsp/Teacher/index");//设置返回页面
					return mv;
				}
				//查找所有系部列表
				List<Category> categoryList =  teacherService.readCategory();
				mv.addObject("categoryList",categoryList);
				mv.addObject("course",course);
				mv.setViewName("jsp/Teacher/modifyCourse");
			}

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return mv;
	}

	
	/**
	 * @author wenli
	 * @param request
	 * @return
	 * 去添加任务页面
	 */
	@RequestMapping(value="toPublishTask")
	@SuppressWarnings({ "unused", "unchecked" })
	public String toPublishTask(HttpServletRequest request) {
		Course course;
		List<String> taskCategoryList=null;
		course = (Course) request.getSession().getAttribute("course");
		try {
			taskCategoryList = teacherService.getTaskCategory();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		request.getSession().setAttribute("taskCategoryList", taskCategoryList);
		request.getSession().setAttribute("courseId", course.getCourseId());
		return "/jsp/Teacher/teacher-release-task";
	}
	/**
	 * @author wenli
	 * @param request
	 * @return
	 * 发布任务
	 */
	@RequestMapping(value="publishTask")
	@SuppressWarnings({ "unused", "unchecked" })
	public String publishTask(HttpServletRequest request) {
		String taskId =  Common.uuid();	//设置任务id
		Object[] obj = Common.fileFactory(request,taskId);
		Map<String, Object> formdata = (Map<String, Object>) obj[1];
		List<File> returnFileList = (List<File>) obj[0]; // 要返回的文件集合
		// 创建list集合用于获取文件上传返回路径名
		List<String> list = new ArrayList<String>();
		List<Accessory> accessories  = new ArrayList<Accessory>();
		List<Resource> resources = new ArrayList<Resource>();
		Task task=new Task();
		task.setTaskId(taskId);
		task.setTaskTitle((String) formdata.get("taskTitle"));
		task.setTaskDetail((String) formdata.get("taskDetail"));
		task.setTaskEndTime(Timestamp.valueOf((String) formdata.get("taskEndTime")));
		task.setTaskType((String) formdata.get("taskType"));
		task.setPublisherId((String) request.getSession().getAttribute("teacherId"));
		task.setPublishTime(new Timestamp(System.currentTimeMillis()));
		task.setVirtualClassNum((String) request.getSession().getAttribute("virtualClassNum"));
		task.setCourseId((String) request.getSession().getAttribute("courseId"));
		task.setTaskType((String) formdata.get("taskCategory"));
		task.setStatus(0);

		try {
			teacherService.createTask(task);		//创建任务
			teacherService.mapClassTask(task.getVirtualClassNum(), taskId);		//映射班级任务表

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(!returnFileList.isEmpty()) {

			for (File file : returnFileList) {
				Accessory accessory = new Accessory();
				accessory.setAccessoryName(file.getName());
				accessory.setAccessoryPath(file.getPath());
				accessory.setTaskId(taskId);
				accessory.setAccessoryTime(Common.TimestamptoString());
				accessories.add(accessory);
				Resource resource = new Resource();
				resource.setResourceId(Common.uuid());
				resource.setResourceName(file.getName());
				resource.setResourceDetail(null);
				resource.setPublishTime(new Timestamp(System.currentTimeMillis()));
				resource.setPublisherId((String) request.getSession().getAttribute("teacherId"));
				resource.setResourceTypeId(Common.fileType(file.getName(), teacherService));//需要判断文件类型
				resource.setResourcePath(file.getPath());
				resource.setCourseId((String) request.getSession().getAttribute("courseId"));
				resource.setSize(file.length()/1024.0+"KB");
				resources.add(resource);
			}
			try {
				teacherService.addAccessory(accessories);	//添加任务附件
				resourceService.upLoadResource(resources);//添加资源
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}


		request.getSession().removeAttribute("courseId");
		return "redirect:/teacher/toPublishTask";

	}
	@RequestMapping(value="toPublishResource")
	@SuppressWarnings({ "unused", "unchecked" })
	public String toPublishResource(HttpServletRequest request) {
		Course course;
		course = (Course) request.getSession().getAttribute("course");
		request.getSession().setAttribute("courseId", course.getCourseId());
		return "/jsp/Teacher/teacher-release-resource";
	}

	@RequestMapping(value="publishResource")
	@SuppressWarnings({ "unused", "unchecked" })
	public String publishResource(HttpServletRequest request) {
		try {
		String resourceId = Common.uuid();
		Object[] obj = Common.fileFactory(request,resourceId);
		Map<String, Object> formdata = (Map<String, Object>) obj[1];
		List<File> returnFileList = (List<File>) obj[0]; // 要返回的文件集合
		Resource resource = new Resource();
		resource.setResourceId(resourceId);
		resource.setCourseId((String) request.getSession().getAttribute("courseId"));
		resource.setPublisherId((String) request.getSession().getAttribute("teacherId"));
		resource.setPublishTime(new Timestamp(System.currentTimeMillis()));
		resource.setResourceDetail((String) formdata.get("resourceDetail"));
		resource.setResourceName((String) formdata.get("resourceName"));
		resource.setResourcePath(returnFileList.get(0).getPath());
		resource.setSize(returnFileList.get(0).length()/1024.0+"KB");
		resource.setResourceTypeId(Common.fileType(returnFileList.get(0).getName(), teacherService));//需要判断文件类型
		teacherService.addResource(resource);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "redirect:/teacher/toPublishResource";

	}

	/**
	 * 添加教师的方法  excel 相关的操作,将数据插入到数据库 
	 * 使用spring的MultipartFile上传文件
	 * */
	@RequestMapping(value="DoExcel",method= {RequestMethod.POST})
	public ModelAndView DoExcel(@RequestParam(value="file_excel") MultipartFile file,HttpServletRequest request) {			
		ModelAndView mv = new ModelAndView();
		String readResult =null;
		try {
			//调用ITeacherService 下的方法，完成增加教师
			//readResult = teacherService.addTeacherInfo(file);
		} catch (Exception e) {
			e.printStackTrace();
		}
		mv.addObject("readResult", readResult);//返回信息
		mv.setViewName("/success");//设置返回页面
		return mv;
	}

	/**
	 * @author wenli
	 * @return
	 * 教师通过点击课程显示自己所带班级
	 */
	@RequestMapping(value="teacherClassList/{courseId}",method= {RequestMethod.GET})
	public ModelAndView teacherClassList(@PathVariable String courseId) {
		ModelAndView mv = new ModelAndView();
		System.out.println(courseId+"dsfghjkljhgfds");
		List<VirtualClass> virtualClassList = null;
		try {
			virtualClassList = teacherService.virtualsForCourse(String.valueOf(courseId));//根据课程ID显示该课程所带班级
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		mv.addObject("virtualClassList", virtualClassList);
		mv.setViewName("/jsp/Teacher/teacherClassList");
		return mv;
	}

	/**
	 * @author wenli
	 * @param request
	 * @return
	 * 跳转到班级详情页
	 */
	@RequestMapping(value="toClassDetail",method= {RequestMethod.GET})
	public ModelAndView toClassDetail(HttpServletRequest request  ,@RequestParam(value="virtualClassNum") String virtualClassNum,@RequestParam(value="virtualClassName") String virtualClassName ) {
		ModelAndView mv = new ModelAndView();
		request.getSession().setAttribute("virtualClassNum", virtualClassNum);
		request.getSession().setAttribute("virtualClassName", virtualClassName);
		Teacher teacher = (Teacher) request.getSession().getAttribute("teacher");
		Student student = (Student) request.getSession().getAttribute("student");
		request.setAttribute("teacher", teacher);
		request.setAttribute("student", student);
		mv.addObject("virtualClassName",virtualClassName);
		mv.setViewName("/jsp/Teacher/teacher-task");
		return mv;

	}
	/**
	 * @author wenli
	 * @param request
	 * @param virtualClassNuM
	 * @return
	 * 显示该班级所有任务列表
	 */
	@RequestMapping(value="teacherTaskList",method= {RequestMethod.GET})
	public ModelAndView teacherTaskList(HttpServletRequest request) {
		ModelAndView mv = new ModelAndView();
		List<String> taskIdList;
		List<Task> taskList;
		String readResult =null;
		Integer point=0;
		String virtualClassNum = (String) request.getSession().getAttribute("virtualClassNum");
		try {
			taskIdList = teacherService.searchTaskId(virtualClassNum);//根据虚拟班级号获得任务列表
			if(!taskIdList.isEmpty()) {
				taskList = teacherService.TaskList(taskIdList);	//根据任务ID号获得任务实体
				for (Task task : taskList) {
					point = teacherService.searchTaskPoint(task.getTaskType());//任务实体对象加入任务分值信息
					task.setAccessoryList(teacherService.searchAccessory(task.getTaskId()));
					task.setTaskPoint(point);
				}
				mv.addObject("taskList", taskList);
			}else {
				mv.addObject("taskList", null);
			}
			Teacher teacher = (Teacher) request.getSession().getAttribute("teacher");
			Student student = (Student) request.getSession().getAttribute("student");
			mv.addObject("teacher",teacher);
			mv.addObject("student",student);
			mv.addObject("readResult", readResult);
			mv.setViewName("/jsp/Teacher/teacher-tasklist");

		} catch (Exception e) {
			e.printStackTrace();
		}
		return mv;	
	}
	/**
	 * @author wenli
	 * @param request
	 * @param virtualClassNum
	 * @return
	 * 根据不同任务类型显示任务列表
	 */
	@RequestMapping(value="teacherTaskListByTaskCategory",method= {RequestMethod.GET})
	public ModelAndView teacherTaskListByTaskCategory(HttpServletRequest request ,@RequestParam(value="taskCategory") String taskCategory) {
		ModelAndView mv = new ModelAndView();
		List<String> taskIdList=new ArrayList<String>();
		List<Task> taskList=new ArrayList<Task>();
		String readResult =null;
		Integer point=0;
		String virtualClassNum = (String) request.getSession().getAttribute("virtualClassNum");
		try {
			taskIdList = teacherService.searchTaskId(virtualClassNum);//根据虚拟班级号获得任务列表
			if(!taskIdList.isEmpty()) {
				taskList = teacherService.teacherTaskAssortmentList(taskIdList,taskCategory);	//根据任务ID号获得任务实体
				if (!taskList.isEmpty()) {
					for (Task task : taskList) {
						point = teacherService.searchTaskPoint(task.getTaskType());//任务实体对象加入任务分值信息
						task.setAccessoryList(teacherService.searchAccessory(task.getTaskId()));
						task.setTaskPoint(point);
					}
					mv.addObject("taskList", taskList);
				}else {
					mv.addObject("taskList", null);
				}
			}else {
				mv.addObject("taskList", null);
			}
			mv.addObject("readResult", readResult);
			if(taskCategory.equals("trial")) {

				mv.setViewName("/jsp/Teacher/teacher-experiment");
			}

			if(taskCategory.equals("course_design")) {
				mv.setViewName("/jsp/Teacher/teacher-tasklist");
			}
			if(taskCategory.equals("work")) {
				mv.setViewName("/jsp/Teacher/teacher-tasklist");
			}


		} catch (Exception e) {
			e.printStackTrace();
		}
		return mv;	
	}
	@RequestMapping(value="toTaskDetail/{taskId}",method= {RequestMethod.GET})
	public ModelAndView toTaskDetail(HttpServletRequest request,@PathVariable String taskId) {
		ModelAndView mv = new ModelAndView();
		Task task ;
		Integer point=0;
		try {
			task = teacherService.searchTask(taskId);
			point = teacherService.searchTaskPoint(task.getTaskType());//任务实体对象加入任务分值信息
			task.setAccessoryList(teacherService.searchAccessory(task.getTaskId()));
			task.setTaskPoint(point);
			mv.addObject("task",task);
			mv.setViewName("/jsp/Teacher/teacher-taskDetail");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			mv.setViewName("jsp/Teacher/teacher-task");
		}
		return mv;
	}

	/**
	 * @author wenli
	 * @param request
	 * @param task_category
	 * @return
	 * 根据不同作业类型查找
	 */
	@RequestMapping(value="teacherTaskAssortmentList/{taskCategory}",method= {RequestMethod.GET})
	public ModelAndView teacherTaskAssortmentList(HttpServletRequest request,@PathVariable String taskCategory) {
		ModelAndView mv = new ModelAndView();
		List<String> taskIdList;
		List<Task> taskList;
		String readResult =null;
		Integer point=0;
		try {
			taskIdList = teacherService.searchTaskId("E56FE27F03344091BE8BDD698426EC22");//根据虚拟班级号获得任务列表
			taskList = teacherService.teacherTaskAssortmentList(taskIdList,taskCategory);	//根据任务ID号获得任务实体
			for (Task task : taskList) {
				point = teacherService.searchTaskPoint(task.getTaskType());//任务实体对象加入任务分值信息
				task.setTaskPoint(point);
			}
			mv.addObject("taskList", taskList);
			mv.addObject("readResult", readResult);
			mv.setViewName("/jsp/Teacher/classTask");

		} catch (Exception e) {
			e.printStackTrace();
		}
		return mv;
	}
	/**
	 * @author LiMing
	 * 通过分类筛选 课程
	 * */
	@RequestMapping(value="readCourseInfoByCategory/{categoryId}",method= {RequestMethod.GET})
	public ModelAndView readCategoryInfo(@PathVariable String categoryId) {			
		ModelAndView mv = new ModelAndView();
		String category = categoryId;
		List<Course> list = new ArrayList<Course>();
		List<String> teacherNames = new ArrayList<String>();
		try {
			list = teacherService.readCourseInfoByCategory(category);
			for (Course course : list) {
				teacherNames.add(teacherService.getTeacherNameById(course.getPublisherId()));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		mv.addObject("teacherNames",teacherNames);
		mv.addObject("categories",categories);
		mv.addObject("courseList", list);
		mv.setViewName("/jsp/CourseJsp/courseSecond");//设置返回页面
		return mv;
	}

	/**
	 * @author wenli
	 * @param request
	 * @return
	 * 进入我的相关班级的iframe入口
	 */
	@RequestMapping(value="toMyCourse")
	public ModelAndView toMyCourse(HttpServletRequest request) {
		ModelAndView mv = new ModelAndView();
		mv.setViewName("/jsp/Teacher/teacherInfo/teacher_course_iframe");
		return mv;
	}
	/**
	 * @author LiMing
	 * @param request
	 * @return
	 * 查找对应老师的课程列表，加入
	 * @throws Exception 
	 */
	@RequestMapping(value="toMyJoinCourse")
	public ModelAndView toMyJoinCourse(HttpServletRequest request) throws Exception {
		ModelAndView mv = new ModelAndView();
		List<String> courseIdListByOthers;		//加入别人的课程ID号
		List<Course> courseListByOthers = null;		//别人课程实体
		List<Teacher> teacherList = null;
		List<String> teacherNames = new ArrayList<String>();
		System.out.println(request.getSession().getAttribute("teacherId"));
		try {
			courseIdListByOthers =teacherService.courseIdList((String) request.getSession().getAttribute("teacherId"), 0);
			courseListByOthers = teacherService.courseList(courseIdListByOthers);
			if(courseListByOthers!=null) {

				for (Course course : courseListByOthers ) {
					teacherList = teacherService.getTeachersByCourseId(course.getCourseId());
					course.setTeacherList(teacherList);
				}
				for (Course course : courseListByOthers) {
					teacherNames.add(teacherService.getTeacherNameById(course.getPublisherId()));
				}
			}


		} catch (Exception e) {
			e.printStackTrace();
		}
		mv.addObject("teacherNames",teacherNames);
		mv.addObject("courseList", courseListByOthers);
		mv.setViewName("/jsp/Teacher/teacherInfo/mycourse_jion");
		return mv;
	}

	/**
	 * @author LiMing
	 * @param request
	 * @return
	 * 查找对应老师的课程列表，关注
	 * @throws Exception 
	 */
	@RequestMapping(value="toMyInterestCourse")
	public ModelAndView toMyInterestCourse(HttpServletRequest request) throws Exception {
		ModelAndView mv = new ModelAndView();
		List<String> courseIdListByOthers;		//加入别人的课程ID号
		List<Course> courseListByOthers = null;		//别人课程实体
		List<Teacher> teacherList = null;
		List<String> teacherNames = new ArrayList<String>();
		System.out.println(request.getSession().getAttribute("teacherId"));
		try {
			courseIdListByOthers =teacherService.courseIdList((String) request.getSession().getAttribute("teacherId"), 2);
			courseListByOthers = teacherService.courseList(courseIdListByOthers);
			if(courseListByOthers!=null) {

				for (Course course : courseListByOthers ) {
					teacherList = teacherService.getTeachersByCourseId(course.getCourseId());
					course.setTeacherList(teacherList);
				}
				for (Course course : courseListByOthers) {
					teacherNames.add(teacherService.getTeacherNameById(course.getPublisherId()));
				}
			}


		} catch (Exception e) {
			e.printStackTrace();
		}
		mv.addObject("teacherNames",teacherNames);
		mv.addObject("courseList", courseListByOthers);
		mv.setViewName("/jsp/Teacher/teacherInfo/mycourse_interest");
		return mv;
	}

	/**
	 * @author Huang
	 * @param request
	 * @return
	 * 查找对应老师的课程列表，创建
	 * @throws Exception 
	 */
	@RequestMapping(value="toMyCreateCourse")
	public ModelAndView toMyCreateCourse(HttpServletRequest request) throws Exception {
		ModelAndView mv = new ModelAndView();
		List<String> courseIdListforMe ;	//自己创建的课程ID号
		List<Course> courseListforMe = null ;		//自己课程实体
		Teacher teacher = null;
		List<String> teacherIdList = null;
		List<Teacher> teacherList = null;
		//创建老师集合的目的是：课程与创建者的匹配
		List<String> teacherNames = new ArrayList<String>();
		try {
			courseIdListforMe = teacherService.courseIdList((String) request.getSession().getAttribute("teacherId"), 1);

			courseListforMe = teacherService.courseList(courseIdListforMe);
			if(courseListforMe!=null) {
				for (Course course : courseListforMe ) {
					teacherList = teacherService.getTeachersByCourseId(course.getCourseId());
					course.setTeacherList(teacherList);
				}
				for (int i = 0; i < courseListforMe.size(); i++) {
					teacher = (Teacher) request.getSession().getAttribute("teacher");
					teacherNames.add(teacher.getTeacherName());
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		mv.addObject("teacherNames",teacherNames);
		mv.addObject("courseList", courseListforMe);
		mv.setViewName("/jsp/Teacher/teacherInfo/mycourse_create");
		return mv;
	}
	/**
	 * @author wenli
	 * @param request
	 * @return
	 * 到个人中心的班级列表页
	 */
	@RequestMapping(value="toMyClassList")
	public ModelAndView toMyClassList(HttpServletRequest request) {
		ModelAndView mv = new ModelAndView();
		List<Term> termList = null;
		List<VirtualClass> virtualClassList= null;
		List<RealClass> realClassList = null;
		List<String> realClassIdList = null;
		String creatorId = (String) request.getSession().getAttribute("teacherId");
		virtualClassList = teacherService.getVirtualClassByCreatorId(creatorId);
		if(virtualClassList!=null) {
			for (VirtualClass virtualClass : virtualClassList) {

				realClassList = teacherService.getRealClassList(virtualClass.getVirtualClassNum());
				virtualClass.setRealClassList(realClassList);
			}

		}

		try {
			termList = teacherService.readTerm();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		mv.addObject("termList", termList);
		mv.addObject("virtualClassList", virtualClassList);
		mv.setViewName("jsp/Teacher/teacherInfo/teacher_class_iframe");
		return mv;
	}

	@RequestMapping(value="toMyInfo")
	public ModelAndView toMyInfo(HttpServletRequest request) {
		ModelAndView mv = new ModelAndView();
		Teacher teacher = (Teacher) request.getSession().getAttribute("teacher");
		mv.addObject("teacher", teacher);
		mv.setViewName("jsp/Teacher/teacherInfo/teacher_myInfo_iframe");
		return mv;
	}

	/**
	 * @author WenLi
	 * @param request
	 * @return
	 * 查找对应老师的课程列表，创建
	 * @throws Exception 
	 */
	//	@RequestMapping(value="searchCourse")
	//	public JSONObject searchCourse(HttpServletRequest request,HttpServletResponse response) throws Exception {
	//		JSONObject jsonObject = new JSONObject();
	//		String content = request.getParameter("content");
	//		List<Course> courseList = null ;
	//		//通过课程名查询课程，由于课程名不重复，故只取返回集合中的第一个
	//		courseList = teacherService.readCourse(content);
	//		Course course = courseList.get(0);
	//	
	//		jsonObject.toString();
	//		response.getWriter().print(course);
	//		return jsonObject;
	//	}
	//	@RequestMapping("/picShow")
	//    public void picShow(HttpServletRequest request,HttpServletResponse response,String picName) throws IOException {
	//		String path = Common.readProperties("path");
	//        String imagePath = path+picName;
	//        response.reset();
	//        //判断文件是否存在
	//        File file = new File(imagePath);
	//        if (!file.exists()) {
	//            imagePath = path+"/"+"course1.jpg";
	//        }
	//        // 得到输出流
	//        OutputStream output = response.getOutputStream();
	//        if (imagePath.toLowerCase().endsWith(".jpg"))// 使用编码处理文件流的情况：
	//        {
	//            response.setContentType("image/jpeg;charset=GB2312");// 设定输出的类型
	//            // 得到图片的真实路径
	//            // 得到图片的文件流
	//            InputStream imageIn = new FileInputStream(new File(imagePath));
	//            // 得到输入的编码器，将文件流进行jpg格式编码
	//            JPEGImageDecoder decoder = JPEGCodec.createJPEGDecoder(imageIn);
	//            // 得到编码后的图片对象
	//            BufferedImage image = decoder.decodeAsBufferedImage();
	//            // 得到输出的编码器
	//            JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(output);
	//            encoder.encode(image);// 对图片进行输出编码
	//            imageIn.close();// 关闭文件流
	//        }
	//        output.close();
	//    }
	@RequestMapping(value="/picShow/{faceImg}")
	@ResponseBody
	public String picShow(HttpServletRequest request,HttpServletResponse response,@PathVariable String faceImg, Model model) {
		// response.setContentType("image/*")

		System.out.println("到这了");
		FileInputStream fis = null;
		OutputStream os = null;
		try {
			fis = new FileInputStream(faceImg);
			os = response.getOutputStream();
			int count = 0;
			byte[] buffer = new byte[1024 * 8];
			while ((count = fis.read(buffer)) != -1) {
				os.write(buffer, 0, count);
				os.flush();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			fis.close();
			os.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "ok";
	}
	/**
	 * @author wenli
	 * @param request
	 * @return
	 * @throws IOException
	 * spring方式下载，当文件较小且下载复杂度不是很大时使用效率较高
	 */
	@RequestMapping("/resourceDownload")
	public ResponseEntity<byte[]> download(HttpServletRequest request,@RequestParam(value="fileName")String fileName,@RequestParam(value="id")String id) throws IOException {

		System.out.println(fileName);
		File file = new File(Common.readProperties("path")+"/"+id+"/"+fileName);
		byte[] body = null;
		InputStream is = new FileInputStream(file);
		body = new byte[is.available()];
		is.read(body);
		HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Disposition", "attchement;filename=" + file.getName());
		HttpStatus statusCode = HttpStatus.OK;
		ResponseEntity<byte[]> entity = new ResponseEntity<byte[]>(body, headers, statusCode);
		return entity;
	}

	/**
	 * @author LiMing
	 * @param request
	 * 访问资源更新页
	 * @throws Exception 
	 */
	@RequestMapping("/toResourceMain")
	public ModelAndView toResourceMain() throws Exception {
		ModelAndView mv = new ModelAndView();
		List<ResourceType> resourceList = new ArrayList<ResourceType>();
		resourceList = teacherService.readResourceCategoried();
		Map<Integer,String> resourceCategories = new HashMap<Integer, String>();
		String resourceName = null;
		for (ResourceType resourceType : resourceList) {
			if(resourceType.getResourceType().equals("word"))
			{
				resourceName = "文档";
			}
			if(resourceType.getResourceType().equals("audio"))
			{
				resourceName = "音频";
			}
			if(resourceType.getResourceType().equals("video"))
			{
				resourceName = "视频";
			}
			if(resourceType.getResourceType().equals("photo"))
			{
				resourceName = "图片";
			}
			if(resourceType.getResourceType().equals("excel"))
			{
				resourceName = "表格";
			}
			if(resourceType.getResourceType().equals("ppt"))
			{
				resourceName = "PPT";
			}
			if(resourceType.getResourceType().equals("pdf"))
			{
				resourceName = "PDF";
			}
			if(resourceType.getResourceType().equals("compressed"))
			{
				resourceName = "压缩文件";
			}
			if(resourceType.getResourceType().equals("other"))
			{
				resourceName = "其他";
			}
			resourceCategories.put(resourceType.getResourceTypeId(),resourceName);
		}
		mv.addObject("resourceCategories", resourceCategories);
		mv.setViewName("/jsp/Teacher/managerResourceList");
		return mv; 
	}

	/**
	 * @author LiMing
	 * @param request
	 * 访问作业资源,作为默认Iframe的显示内容
	 */
	@RequestMapping("/toResource/{resourceType}")
	public ModelAndView toResource(@PathVariable String resourceType) throws IOException {
		ModelAndView mv = new ModelAndView();
		List<Resource> list = new ArrayList<Resource>();
		int type = Integer.parseInt(resourceType);
		//showResourceByType()为混合方法，看具体注释
		list  = resourceService.showResourceByType(type);
		mv.addObject("resource", list);
		mv.setViewName("/jsp/Teacher/managerResourceListIframe");
		return mv; 
	}

	/**
	 * @author LiMing
	 * @param request
	 * 更新资源
	 * @throws Exception 
	 */
	@RequestMapping("/toUpdateResource/{resourceId}")
	public ModelAndView toUpdateResource(@PathVariable String resourceId,@RequestParam(value="resourceName")String resourceName,@RequestParam(value="resourceDetail")String resourceDetail) throws Exception {
		ModelAndView mv = new ModelAndView();
		resourceService.updateResource(resourceId, resourceName, resourceDetail, null, null, null, null, null, null, null, null, null);
		mv = toResourceMain();
		return mv; 
	}

	/**
	 * @author LiMing
	 * @param request
	 * 删除资源
	 * @throws Exception 
	 */
	@RequestMapping("/toDeleteResource/{resourceId}")
	public String toDeleteResource(@PathVariable String resourceId) throws Exception {
		String msg = null;
		msg  = resourceService.deleteResourceById(resourceId);
		return msg;
	}

	/**
	 * @author LiMing
	 * @param request
	 * 返回需要更新的资源，将对象显示在模态框
	 * @throws Exception 
	 */
	@RequestMapping("/toModalResource/{resourceId}")
	public void tomodalResource(HttpServletRequest request, HttpServletResponse response,@PathVariable String resourceId) throws Exception {
		String msg = null;
		List<Resource> list = new ArrayList<Resource>();
		list  = resourceService.showResource(resourceId);//此时resourceId 不为空，将按照条件查询。返回只有一个对象的集合
		JSONArray  json  =  JSONArray.fromObject(list); 
		String result = json.toString();
		response.getWriter().print(result);
	}

	/**
	 * 跳转到教师主页
	 * @param request
	 * @return
	 */
	@RequestMapping(value="toTeacherPage")
	public ModelAndView toTeacherPage(HttpServletRequest request){
		ModelAndView mv = new ModelAndView();
		try {
			//获取教师信息
			Teacher teacher = (Teacher) request.getSession().getAttribute("teacher");
			mv.addObject("teacher",teacher);
			mv.setViewName("jsp/Teacher/teacherInfo/centreForTeacher"); //跳转教师个人页
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			mv.addObject("readResult", "异常");//返回信息
			mv.setViewName("/jsp/Teacher/index");//设置返回页面
		}
		return mv;
	}


}
