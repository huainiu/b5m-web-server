<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>访问日志</title>
<style type="text/css">
td{word-wrap:break-word;overflow:hidden;}
</style>
</head>
<body>
   <div>
   	   <form action="accesslog.html">
	   	   <table>
	   	   		<tr>
	   	   			<td>server:<input type="text" name="server" value="${server}"></td>
	   	   		</tr>
	   	   		<tr>
	   	   			<td>当前页:<input type="text" name="currentPage" value="${currentPage}"></td>
	   	   		</tr>
	   	   		<tr>
	   	   			<td>展示个数:<input type="text" name="pageSize" value="${pageSize}"></td>
	   	   		</tr>
	   	   		<tr>
	   	   			<td>path:<input type="text" name="path" value="${path}"></td>
	   	   		</tr>
	   	   		<tr>
	   	   			<td>来源:<input type="text" name="ref" value="${ref}"></td>
	   	   		</tr>
	   	   		<tr>
	   	   			<td>来源不包括:<input type="text" name="_ref" value="${_ref}"></td>
	   	   		</tr>
	   	   		<tr>
	   	   			<td>参数:<input type="text" name="params" value="${params}"></td>
	   	   		</tr>
	   	   		<tr>
	   	   			<td><input type="submit"></td>
	   	   		</tr>
	   	   </table>
   	   </form>
   </div>
   <h1>${server}</h1>
   <table style="border: 1px solid black;">
	   <c:forEach items="${mapList}" var="map">
	    	<tr>
	    		<td>${map.server}:</td>
	    		<td>请求数：${map.count}</td>
	    		<td>最长访问时间:${map.max}</td>
	    		<td>平均访问时间:${map.avg}</td>
	    	</tr>
	   </c:forEach>
   </table>
   <br/><br/>
   <div style="float: right;">总记录: ${page.totalRecord}</div>
   <table style="border: 1px solid black;table-layout:fixed;width:100%">
   		<thead>
   			<tr>
   				<td width="100">服务</td>
   				<td width="200">访问路径</td>
   				<td width="120">ip</td>
   				<td class="ly">来源</td>
   				<td>访问时间</td>
   				<td>参数</td>
   				<td width="100">响应时间</td>
   				<td width="200">错误信息</td>
   			</tr>
   		</thead>
   		<tbody>
   			<c:forEach var="recode" items="${page.records}">
   				<tr>
   					<td>${recode.server}</td>
	   				<td>${recode.path}</td>
	   				<td>${recode.ip}</td>
	   				<td class="ly">${recode.ref}</td>
	   				<td>${recode.accessDate}</td>
	   				<td>${recode.params}</td>
	   				<td>${recode.responeTime}</td>
	   				<td>${recode.exception}</td>
	   			</tr>
   			</c:forEach>
   		</tbody>
   </table>
</body>
</html>