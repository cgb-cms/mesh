package com.gentics.cailun.linkrenderer;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.link.CaiLunLinkResolver;
import com.gentics.cailun.core.link.CaiLunLinkResolverFactoryImpl;
import com.gentics.cailun.core.link.LinkReplacer;
import com.gentics.cailun.core.repository.PageRepository;
import com.gentics.cailun.core.rest.model.Page;
import com.gentics.cailun.test.Neo4jSpringTestConfiguration;

@ContextConfiguration(classes = { Neo4jSpringTestConfiguration.class })
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class LinkRendererTest {

	final String content = "some bla START<a href=\"${Page(2)}\">Test</a>   dasasdg <a href=\"${Page(3)}\">Test</a>DEN";

	@Autowired
	CaiLunLinkResolverFactoryImpl<CaiLunLinkResolver> resolverFactory;

	@Autowired
	PageRepository pageRepository;

	@Test
	public void testNodeReplace() throws IOException, InterruptedException, ExecutionException {

		// Create some dummy content
		Page page1 = new Page("test");
		pageRepository.save(page1);
		Page page2 = new Page("test2");
		page2.setContent(content);
		pageRepository.save(page2);

		LinkReplacer<CaiLunLinkResolver> replacer = new LinkReplacer(resolverFactory);
		String out = replacer.replace(content);
		System.out.println(out);
	}

	private String getUrlForPage(Long id) {
		return "/var/www/index.html";
	}

	// @Test
	public void testRendering() throws IOException {
		System.out.println(content);
		int start = content.indexOf("#");
		int stop = content.lastIndexOf(")");
		int len = stop - start;
		System.out.println("from " + start + " to " + stop + " len " + len);
		InputStream in = IOUtils.toInputStream(content);
		try {
			int e = 0;
			for (int c; (c = in.read()) != -1 /* EOF */;) {
				if (e >= start && e < stop) {
					// System.out.println("skipping");
					in.skip(len);
					System.out.print("Hugsdibugsdi");
					e = stop;
				} else {
					System.out.print((char) c);
				}
				e++;
			}
		} finally {
			in.close();
		}
	}

}
