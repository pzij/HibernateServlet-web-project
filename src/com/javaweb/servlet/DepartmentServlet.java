package javaweb.servlet;

import java.io.IOException;
import java.sql.Time;
import java.util.Date;
import java.util.List;

import javax.persistence.TypedQuery;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.sql.Template.NoOpColumnMapper;

import antlr.StringUtils;
import javassist.expr.NewArray;
import javaweb.HibernateUtil;
import javaweb.StringUtil;
import javaweb.bean.Department;
import javaweb.bean.Employee;

/**
 * Servlet implementation class EmployeeServlet
 */
public class DepartmentServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public DepartmentServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		this.doPost(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String action = request.getParameter("action");
		if ("add".equals(action)) {
			add(request,response);
		} else if ("query".equals(action)) {
			query(request, response);
		} else {
			list(request, response);
		}
		
	}

	private void add(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String id=request.getParameter("id");
		String name=request.getParameter("name");
		String description=request.getParameter("description");
		String lineManagerId=request.getParameter("lineManagerId");
		
		RequestDispatcher dispatcher = request.getRequestDispatcher("/addDepartment.jsp");
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction trans = session.beginTransaction();
		List<Department> list = session.createQuery(" from Department d where d.name = :name").setParameter("name", name).list();
		
		if (list.size()>0) {
			request.setAttribute("message", "Department name \""+name+"\" already existed.");
			dispatcher.forward(request, response);
			session.close();
			return;
		}
		
		Department d=new Department();
		d.setName(name);
		d.setDescription(description);
		
		if (!StringUtil.isNull(lineManagerId)) {
			Employee lineManager = (Employee) session.get(Employee.class, new Integer(lineManagerId));
			d.setLineManager(lineManager);
		}
		session.persist(d);
		trans.commit();
		session.close();
		request.setAttribute("message", "Department \""+d.getName()+"\" saved successfully.");
		list(request, response);
	}

	private void query(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String key = request.getParameter("key");
		String hql = " from Employee e ";
		if(!StringUtil.isNull(key)) {
			hql+=" where e.name like '%"+key+"%' ";
		}
		final int SIZE=8;
		Session session = HibernateUtil.getSessionFactory().openSession();
		List<Employee> employeeList=session.createQuery(hql).setMaxResults(SIZE).list();
		session.close();
		
		StringBuffer buffer = new StringBuffer();
		buffer.append("{");
		for (int i = 0; i < employeeList.size(); i++) {
			if (i!=0) {
				buffer.append(",");
			}
			Employee employee = employeeList.get(i);
			buffer.append(employee.getId()+":\""+employee.getName()+"\"");
		}
		
		if(employeeList.size()>SIZE) {
			buffer.append(", 0:\"More than "+SIZE+" messages...\"");
		}
		buffer.append("}");
		response.getWriter().write(buffer.toString());
		
	}

	private void list(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String sort=request.getParameter("sort");
		String order=request.getParameter("order");
		
		if (StringUtil.isNull(sort)) {
			sort="id";
		}
		if (StringUtil.isNull(order)) {
			order="desc";
		}
		
		String departmentName = request.getParameter("departmentName");
		String managerName = request.getParameter("managerName");
		String employeesSize = request.getParameter("employeesSize");
		String employeesOperator = request.getParameter("employeesOperator");

		String where="";
		
		if (!StringUtil.isNull(departmentName)) {
			if (!StringUtil.isNull(where)) 
				where += " and ";
			where += " d.name like '%"+departmentName+"%' ";
		}
	
		if (!StringUtil.isNull(managerName)) {
			if (!StringUtil.isNull(where)) 
				where += " and ";
			where += " d.lineManager.name like '%"+managerName+"%' ";
		}

		if (!StringUtil.isNull(employeesSize)) {
			if (!StringUtil.isNull(where)) 
				where += " and ";
			where += " d.employees.size "+employeesOperator+" "+employeesSize+" ";
		}
	
		String hql = " from Department d ";
		if (!StringUtil.isNull(where)) {
			hql+=" where "+where;
		}
		
		hql += " order by d."+sort+" "+order;
			
		Session session = HibernateUtil.getSessionFactory().openSession();
		List<Department> departmentList = session.createQuery(hql).list();
		
		for (Department d : departmentList) {
			d.getEmployees().size();
		}
		session.close();
		
		request.setAttribute("departmentList", departmentList);
		request.setAttribute("sort", sort);
		request.setAttribute("order", order);
		request.setAttribute("url", StringUtil.getURL(request));
		
		if (request.getAttribute("message")== null) {
			request.setAttribute("message", "HQL: "+hql);
		}
		
		request.getRequestDispatcher("/listDepartment.jsp").forward(request, response);
	}


}
