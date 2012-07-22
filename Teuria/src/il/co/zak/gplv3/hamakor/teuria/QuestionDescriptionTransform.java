/*
    Teuria - Practice for Israeli Driving Theory Test
    Copyright (C) 2012  Omer Zak

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see http://www.gnu.org/licenses/

    The author can be contacted via his Website at http://www.zak.co.il/
*/

/**
 * Transform the Description field of a question from the theory questions database
 * into form that we need.
 */
package il.co.zak.gplv3.hamakor.teuria;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.util.Log;

/**
 * @author omer
 *
 */


public final class QuestionDescriptionTransform {
	private static final String TAG = "QuestionDescriptionTransform";

	// teuria-test needs access to JS_DOLI, so it was declared as public.
	public static final String JS_DOLI = "<script type=\"text/javascript\">"
			+ "function doli(elem) {"
			+   "var oldval = elem.innerHTML;"
			+   "var myid = elem.attributes.getNamedItem(\"id\");"
			+   "if (myid == null) {"
			+     "elem.setAttribute(\"style\",\"background: #ffc0c0\");"
			// Reveal the correct answer
			+     "var correct = document.getElementById(\"T\");"
			+     "correct.setAttribute(\"style\",\"background: #e0ffe0\");"
			+     "androidresult.setResultCode(false);"
			+   "}"
			+   "else {"
			+     "elem.setAttribute(\"style\",\"background: #c0ffc0\");"
			+     "androidresult.setResultCode(true);"
			+   "}"
			// The following removes all onclick attributes so that the user won't be able
			// to guess any more questions.
			+   "var liis = document.getElementsByTagName(\"li\");"
			+   "for (var ind = 0; ind <liis.length; ind++) {"
			+     "liis[ind].removeAttribute(\"onclick\");"
			+   "}"
			+ "}"
			+ "</script>";


	final static private class TransformAuxiliaryFunctions {
		static public String getStringFromDoc(Document doc)    {
			// Source: http://stackoverflow.com/questions/315517/is-there-a-more-elegant-way-to-convert-an-xml-document-to-a-string-in-java-than
			try {
				DOMSource domSource = new DOMSource(doc);
				StringWriter writer = new StringWriter();
				StreamResult result = new StreamResult(writer);
				TransformerFactory tf = TransformerFactory.newInstance();
				Transformer transformer = tf.newTransformer();
				transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
				transformer.transform(domSource, result);
				writer.flush();
				return writer.toString();
			}
			catch(TransformerException ex) {
				ex.printStackTrace();
				return null;
			}			
		}
	}

	
	/**
	 * Transform a question's description.
	 * A question is a HTML fragment, and we want to modify the <li> elements in it.
	 * There are four such elements.  One of them has id="correctAnswerxxx" attribute.  The others don't have any attribute.
	 * We want to transform them as follows.
	 * For correct answers:
	 * <li onclick="thiselement.className='text-correct'; notifyApplication(true);">...</li>
	 * For wrong answers:
	 * <li onclick="thiselement.className='text-wrong'; notifyApplication(false); var correctAnswer=document.getElementById('correctAnswer0442');correctAnswer.className='text-the-correct-answer'">...</li>
	 * 
	 * We want also to add a JavaScript script to set up some things.
	 * Also a CSS fragment is needed.
	 *
	 *
	 * @param description - the input description
	 * @return - the transformed description.
	 */
	public static String transform(String description) {
		try {
			DocumentBuilder builder = DocumentBuilderFactory
					.newInstance().newDocumentBuilder();
			//Document doc = builder.parse(new StringBufferInputStream(description));
			Document doc = builder.parse(new InputSource(new StringReader(description)));
			NodeList liis = doc.getElementsByTagName("li");
			for (int ind = 0; ind < liis.getLength(); ++ind) {
				Element liElement = (Element) liis.item(ind);
				liElement.setAttribute("onclick","doli(this);");
				if (liElement.hasAttribute("id")) {
					// Ensure that the correct answer has a known attribute.
					liElement.setAttribute("id","T");
				}
				else {
					NodeList spans = liElement.getElementsByTagName("span");
					for (int jnd = 0; jnd < spans.getLength(); ++jnd) {
						Element span = (Element) spans.item(jnd);
						if (span.hasAttribute("id")) {
							String attr = span.getAttribute("id");
							if (attr.regionMatches(true, 0, "correctAnswer", 0, 7)) {
								// We ignore case and compare only first 7 characters to
								// guard against possibility of minor format differences

								// Ensure that the correct answer has a known attribute.
								liElement.setAttribute("id","T");
								break;
							}
						}
					}
				}
			}
			NodeList buttons = doc.getElementsByTagName("button");
			if (buttons.getLength() == 1) {
				// We deal with the button only if there is exactly one original button.
				Node parent = buttons.item(0).getParentNode();
				parent.removeChild(buttons.item(0));
			}
			
			description = TransformAuxiliaryFunctions.getStringFromDoc(doc);
		}
		catch (IOException e) {
			Log.d(TAG, "IOException: " + e.toString());
		}
		catch (ParserConfigurationException e) {
			Log.d(TAG, "ParserConfigurationException: " + e.toString());
		}
		catch (SAXException e) {
			Log.d(TAG, "SAXException: " + e.toString());
		}

		description = JS_DOLI + description;
		return description;
	}
}
