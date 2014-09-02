package com.b5m.servlet;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.DispatcherServlet;

import com.b5m.sys.ContextUtils;

public class B5mServerInitDispatchServlet extends DispatcherServlet{
	/**
	 * 
	 */
	private static final Log LOG = LogFactory.getLog(B5mServerInitDispatchServlet.class);
	private static final long serialVersionUID = 7688908942387008669L;
	private static final String DEFAULT_CHANNEL_CONFIG = "classpath:channels.xml";
	private static final String DEFAULT_COMMINFO_PATH = "config.properties";
	private static Properties properties = null;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		initSearchContext(config);
	}

	private void initSearchContext(ServletConfig arg0) throws ServletException{
		String channelsPath = arg0.getInitParameter("ChannelPath");
		if (StringUtils.isEmpty(channelsPath)) {
			channelsPath = DEFAULT_CHANNEL_CONFIG;
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("---------------------start init channels file[" + channelsPath + "]------------------");
		}
		InputStream inputStream = null;
		if (channelsPath.startsWith("classpath:")) {
			inputStream = B5mServerInitDispatchServlet.class.getClassLoader().getResourceAsStream(channelsPath.substring("classpath:".length()));
		} else {
			String filePath = arg0.getServletContext().getRealPath("/") + channelsPath;
			try {
				inputStream = new FileInputStream(filePath);
			} catch (FileNotFoundException e) {
				throw new ServletException("file:[" + channelsPath + "] not exists");
			}
		}
		try {
			initSysProp();
			ContextUtils.init(IOUtils.toString(inputStream), properties);
		} catch (IOException e) {
		}
	}

	private void initSysProp() throws IOException {
		String commInfoProPath = DEFAULT_COMMINFO_PATH;
		InputStream inputStream = B5mServerInitDispatchServlet.class.getClassLoader().getResourceAsStream(commInfoProPath);
		properties = new Properties();
		properties.load(inputStream);
	}
}
