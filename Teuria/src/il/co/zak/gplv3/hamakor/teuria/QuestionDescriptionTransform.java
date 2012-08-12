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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.w3c.dom.Text;
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

	private static final Pattern SPLITTER_PATTERN = Pattern.compile("\\|");
	//private static final Pattern LICENSE_LEVEL_PATTERN = Pattern.compile("^\\s*\\u00AB([a-zA-Z0-9]+)\\u00BB\\s*$");
	private static final Pattern LICENSE_LEVEL_PATTERN = Pattern.compile("^\\s*\\u00AB(\\p{Alnum}+)\\u00BB\\s*$");
	// The simple [a-zA-Z0-9]+ pattern failed because the license level 'B' is actually
	// Cyrillic B.

	/**
	 * Interface to be used to pass strings from a string array, when
	 * the client prefers to access them one by one.
	 * @author omer
	 *
	 */
	public interface ReceiveStrings {
		public void receiveString(String string);
	}

	final static public class TransformAuxiliaryFunctions {
		/**
		 * Exports xml string from the DOM data structure
		 * @param doc - DOM data structure representing the document
		 * @return String containing the corresponding xml. 
		 */
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
		
		/**
		 * Extract an array of license levels from the given string.
		 * @param text
		 * @param receiver if not null, then receiver.receiveString() is called
		 * for each license level string.
		 * @return false if the input text is not a valid license levels string.
		 * The input text is not valid if a license level string is not recognized
		 * in any post-split segment except for the first and last ones.
		 */
		static public boolean extractLicenseLevels(String text, ReceiveStrings receiver) {
			String[] splitted = SPLITTER_PATTERN.split(text);
			int ind = 0;
			//Log.d(TAG,"The string was split into " + splitted.length + " fragments");
			for (String frag: splitted) {
				Matcher match = LICENSE_LEVEL_PATTERN.matcher(frag);
				if (match.matches()) {
					//Log.d(TAG,"Recognized license level >" + match.group(1) + "<");
					if (receiver != null) {
						receiver.receiveString(match.group(1));
					}
				}
				else {
					if ((ind > 0) && (ind < splitted.length - 1)) {
						//Log.d(TAG,"Fragment " + ind + " >" + frag + "< failed to match - returning false");
						//for (int jnd = 0; jnd < frag.length(); ++jnd) {
						//	Log.d(TAG,"character " + jnd + " is " + frag.codePointAt(jnd) + ", " + frag.charAt(jnd) + ".");
						//}
						return false;
					}
					//Log.d(TAG,"Excusing fragment " + ind + " >" + frag + "< from matching a license level pattern");
				}
				++ind;
			}
			return true;
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
			Document doc = builder.parse(new InputSource(new StringReader(description)));
			
			// Transform the <li> elements to invoke our JavaScript code.
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
			
			// Eliminate the button for revealing the correct answer.
			NodeList buttons = doc.getElementsByTagName("button");
			if (buttons.getLength() == 1) {
				// We deal with the button only if there is exactly one original button.
				Node parent = buttons.item(0).getParentNode();
				parent.removeChild(buttons.item(0));
			}
			
			// Eliminate the string with license levels.  It is the last <span> in
			// the question description.
			NodeList spans = doc.getElementsByTagName("span");
			String text = ((Text) spans.item(spans.getLength()-1).getFirstChild()).getWholeText();
			if (TransformAuxiliaryFunctions.extractLicenseLevels(text,null)) {
				Node spanparent = spans.item(spans.getLength()-1).getParentNode();
				spanparent.removeChild(spans.item(spans.getLength()-1));
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
