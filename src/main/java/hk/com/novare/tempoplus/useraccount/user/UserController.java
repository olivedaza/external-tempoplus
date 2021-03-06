package hk.com.novare.tempoplus.useraccount.user;

import hk.com.novare.tempoplus.employee.Employee;
import hk.com.novare.tempoplus.timelogging.DataAccessException;
import java.sql.SQLException;

import hk.com.novare.tempoplus.timelogging.TimeLoggingService;

import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttributes;

@Controller
@RequestMapping("/user")
@SessionAttributes({"userEmployeeId", "userEmail"})

public class UserController {
	
	@Inject UserService userService;
	
	@Inject
	TimeLoggingService timelogService;
	@Inject User user;

	
	private String accessLevel = "";
	
	@RequestMapping(value = "/index", method = RequestMethod.GET)
	public String index(){
	
		return "index";
	}
	
	@RequestMapping(value = "/login	",  method = RequestMethod.POST)
	public String loginValidation(HttpServletRequest httpServletRequest,
			HttpSession session,
			ModelMap modelMap,
			@RequestParam(value = "userName")String userEmail,
			@RequestParam(value = "password") String password) throws DataAccessException {
		
		String redirect = "";
		if(userEmail.equals("") || password.equals("")){
			modelMap.addAttribute("outputMsg", "Input username/pasword.");
			redirect = "index";
		}
		else{
			boolean isValid = userService.validateLogInAccess(userEmail, password);
			
			if(isValid){
				
				timelogService.logTimeIn();
				
				redirect = "redirect:identifyHome";
	
			}else{
				modelMap.addAttribute("outputMsg", "Invalid e-mail address/password.");
			redirect = "index";
			}
		}
		return redirect;
	}
	
	@RequestMapping(value = "/identifyHome", method = RequestMethod.GET)
	public String home(ModelMap modelMap){
		
		modelMap.addAttribute("employeeDetailsList", userService.retrieveUserInformation(user.getEmail()));
		modelMap.addAttribute("supervisorDetails", userService.retrieveSupervisorInformation(user.getSupervisorId()));
		
		//set to session attributes
		modelMap.addAttribute("userEmployeeId", user.getEmployeeId());
		modelMap.addAttribute("userEmail",user.getEmail());
		modelMap.addAttribute("userFirstName", user.getFirstname().toLowerCase());

		modelMap.addAttribute("userEmail", modelMap.get("userEmail"));
		if(user.getDepartmentId() == 11){
			//for HR access

			 accessLevel ="homeHR";
		}
		else if(user.getIsSupervisor() == 1){
			//for Supervisor access
			
			accessLevel = "homeSupervisor";
		}else {
			//for for all access
			
			accessLevel = "homeUser";
		}		
	return accessLevel;		
	}
		
	@RequestMapping("/changePassword")
	public @ResponseBody String changePasswordProcess(
			@RequestParam("currentPassword") String currentPassword,
			@RequestParam("newPassword") String newPassword,
			@RequestParam("repeatNewPassword") String repeatedNewPassword) {
		String message = "";
		boolean isCurrentPasswordMatched = userService.matchCurrentPassword(
						userService.hashPassword(currentPassword),user.getPassword());
		boolean isNewPasswordAcceptable = 
				userService.validateNewPassword(newPassword, repeatedNewPassword);
	
		if(isCurrentPasswordMatched && isNewPasswordAcceptable){
			
			userService.saveNewPassword(user.getEmployeeId(), repeatedNewPassword);
			message = "New password is saved.";
			
		}else{
			message =  "Check your input.";
		}
		return message;
	}
	
	@RequestMapping("/logout")
	public String logout(ModelMap modelMap, HttpServletRequest httpServletRequest) throws SQLException{
	  
	  httpServletRequest.getSession().invalidate();
	  modelMap.clear();
		timelogService.logTimeOut();

	  //clear all objects in memory
	  List<User> userList = userService.clearUserInformation(user.getEmail());
	  List<Employee> supervisorList = userService.clearSupervisorInformation(user.getSupervisorId());
		
	  userList.clear();
	  supervisorList.clear();
	  
	  return "redirect:index";
	 }
}
