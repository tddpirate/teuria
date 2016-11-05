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
 * Fetch from the Web a zip-compressed xml file which is the driving theory questions database. 
 */
package il.co.zak.gplv3.hamakor.teuria;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;
import android.util.Xml;

/**
 * @author omer
 *
 */

public final class QuestionsFetcher {
	private static final String TAG = "QuestionsFetcher";

	/**
	 * Listener for questions as they are retrieved from the file, one by one.
	 * @author omer
	 *
	 */
	public interface QuestionListener {
		/**
		 * Called whenever a Question is available (such as for saving to database).
		 * @param question - the Question class instance to be saved.
		 */
		public void listenNewQuestion(Question question);
		
		/**
		 * Called whenever a license level string is available (such as for saving
		 * to the database).  The license level is associated with the most recently
		 * provided question (last listenNewQuestion() call).
		 * @param licenseLevel - another license level for the question.
		 */
		public void listenLicenseLevel(String licenseLevel);
		
		/**
		 * Called to indicate that all questions have been processed.
		 */
		public void endOfQuestions();
	}

	/**
	 * 
	 * @param urlstring - from which the xml file containing the questions is to be retrieved
	 */
	private static String retrieveZippedXmlFile(String urlstring) throws MalformedURLException,IOException {
		URL url = new URL(urlstring);
		HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
		int responseCode = urlConnection.getResponseCode();
		if (200 != responseCode) {
			throw new RuntimeException("Bad URL: " + urlstring + "(Response code: " + responseCode + ")");
		}
		InputStream in = urlConnection.getInputStream();
		ZipInputStream zipin = new ZipInputStream(new BufferedInputStream(in,32768));
		ZipEntry ze = zipin.getNextEntry();  // We expect to see exactly one entry.
		if (null == ze) {
			throw new RuntimeException("No ZIP archive in URL: " + urlstring);
		}
		String xmlfname = ze.getName();
		if (!xmlfname.substring(xmlfname.length() - 4).contentEquals(".xml")) {
			throw new RuntimeException("No XML file in ZIP archive in URL: " + urlstring);
		}
		
		byte[] xmlbytes = new byte[(int) ze.getSize()];
		int read_offset = 0;
		while (read_offset < xmlbytes.length) {
			int read_bytes = zipin.read(xmlbytes, read_offset, xmlbytes.length - read_offset);
			if (read_bytes == 0) {
				throw new RuntimeException("ZIP read error");
			}
			read_offset += read_bytes;
		}
		// Note: I looked for a way to have zipin feed directly to a XML decoder stream,
		// which will finish parsing at end of ze.getSize() bytes, but found no way to do
		// this.
		String xmlstuff = new String(xmlbytes,"UTF-8");
		zipin.closeEntry();
		/*
		if (null != zipin.getNextEntry()) {
			throw new RuntimeException("More than one file in ZIP archive in URL: " + urlstring);
		}
		*/
		zipin.close();
		
		return xmlstuff;
	}

    /**
     *
     * @param urlstring - from which the xml file containing the questions is to be retrieved
     */
    private static String retrieveXmlFile(String urlstring) throws MalformedURLException,IOException {
        URL url = new URL(urlstring);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        int responseCode = urlConnection.getResponseCode();
        if (200 != responseCode) {
            throw new RuntimeException("Bad URL: " + urlstring + "(Response code: " + responseCode + ")");
        }
        long contentLength = Long.parseLong(urlConnection.getHeaderField("Content-Length"));
        if (contentLength < 1) {
            throw new RuntimeException("Bad content length for URL: " + urlstring);
        }
        InputStream in = urlConnection.getInputStream();
        BufferedInputStream bufin = new BufferedInputStream(in,32768);

        byte[] xmlbytes = new byte[(int) contentLength];
        int read_offset = 0;
        while (read_offset < xmlbytes.length) {
            int read_bytes = bufin.read(xmlbytes, read_offset, xmlbytes.length - read_offset);
            if (read_bytes == 0) {
                throw new RuntimeException("Internal read error");
            }
            read_offset += read_bytes;
        }
        String xmlstuff = new String(xmlbytes,"UTF-8");
        bufin.close();

        return xmlstuff;
    }

	private static class QuestionsContentHandler extends DefaultHandler {
		private QuestionListener mListener;
		private StringBuilder mText = new StringBuilder(4096);
		private String mCurrentTag = new String();
		private Question mCurrentQuestion = new Question();
		
		public QuestionsContentHandler(QuestionListener listener) {
			super();
			mListener = listener;
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			super.characters(ch, start, length);
			mText.append(ch, start, length);
		}

		/* (non-Javadoc)
		 * @see org.xml.sax.helpers.DefaultHandler#ignorableWhitespace(char[], int, int)
		 */
		/*
		@Override
		public void ignorableWhitespace(char[] ch, int start, int length)
				throws SAXException {
			super.ignorableWhitespace(ch, start, length);
			mText.append(ch, start, length);
		}
		 */
		
		/* (non-Javadoc)
		 * @see org.xml.sax.helpers.DefaultHandler#endDocument()
		 */
		@Override
		public void endDocument() throws SAXException {
			super.endDocument();
			mListener.endOfQuestions();
		}

		/* (non-Javadoc)
		 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
		 */
		@Override
		public void endElement(String uri, String localName, String qName)
				throws SAXException {

			if (localName == "item") {
				if (mText.length() > 0) {
					throw new SAXException("Text found outside of item's subordinate tags");
				}
				mListener.listenNewQuestion(mCurrentQuestion);
				QuestionDescriptionTransform.TransformAuxiliaryFunctions.extractLicenseLevels(mCurrentQuestion.getDescription(),new QuestionDescriptionTransform.ReceiveStrings() {
					public void receiveString(String string) {
						mListener.listenLicenseLevel(string);
					}
				});
				mCurrentQuestion.clear();
			}
			else if (!mCurrentQuestion.isClear()) {
				// Recognize the other tags only inside an item.
				if (mCurrentTag != localName) {
					throw new SAXException("Bad tag end - should be " + mCurrentTag + ", is " + localName + " instead");
				}

				if (localName == "title") {
					mCurrentQuestion.setTitle(mText.toString());
				}
				else if (localName == "link") {
					mCurrentQuestion.setLink(mText.toString());
				}
				else if (localName == "guid") {
					mCurrentQuestion.setGuid(mText.toString());
				}
				else if (localName == "description") {
					mCurrentQuestion.setDescription(mText.toString());
				}
				else if (localName == "author") {
					mCurrentQuestion.setAuthor(mText.toString());
				}
				else if (localName == "category") {
					mCurrentQuestion.setCategory(mText.toString());
				}
				else if (localName == "pubDate") {
					try {
						mCurrentQuestion.setPubDate(mText.toString());
					}
					catch (ParseException e) {
						throw new SAXException("parse exception for " + mText.toString());
					}
				}
			}
			mText.delete(0, mText.length());
			super.endElement(uri, localName, qName);
		}


		/* (non-Javadoc)
		 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
		 */
		@Override
		public void startElement(String uri, String localName, String qName,
				Attributes attributes) throws SAXException {
			// TODO Auto-generated method stub
			super.startElement(uri, localName, qName, attributes);

			if (mText.length() > 0) {
				throw new SAXException("Text outside of tags: '" + mText + "'");
			}
			
			if (localName == "item") {
				if (!mCurrentQuestion.isClear()) {
					throw new SAXException("item inside item");
				}
				mCurrentQuestion.setTitle(""); // Just to mark the question as !isClear().
			}
			else if (!mCurrentQuestion.isClear()) {
				// Recognize the other tags only inside an item.
				
				if ((localName == "title")
						|| (localName == "link")
						|| (localName == "guid")
						|| (localName == "description")
						|| (localName == "author")
						|| (localName == "category")
						|| (localName == "pubDate")) {
					mCurrentTag = localName;
					mText.delete(0,mText.length());
				}
			}
		}
	}
	
	/**
	 * Parse the xml file and return questions from it, one by one.
	 * @param xmlstuff - the xml file contents
	 * @param listener - listener to receive the questions
	 * 
	 * Was declared as public to support testing.
	 */
	public static void processZippedXmlFile(String xmlstuff, QuestionListener listener, XmlParsingProgress progressbar)
			throws IOException,SAXException {
		org.xml.sax.ContentHandler contentHandler = new QuestionsContentHandler(listener);
		//Xml.parse(xmlstuff, contentHandler);
		CountingStringReader reader = new CountingStringReader(xmlstuff, progressbar);
		Xml.parse(reader, contentHandler);
	}
	
	/**
	 * Retrieve questions xml file and return the questions one by one via the listener.
	 * @param urlstring
	 * @param listener
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws SAXException
	 */
	public static void fetchQuestionsDatabase(String urlstring, QuestionListener listener, XmlParsingProgress progressbar)
			throws MalformedURLException,IOException,SAXException {
		long startReadingTimeStamp = System.currentTimeMillis();
		String xmlstuff = (urlstring.substring(urlstring.length() - 4).contentEquals(".zip")) ? retrieveZippedXmlFile(urlstring) : retrieveXmlFile(urlstring);
		long startParsingTimeStamp = System.currentTimeMillis();
		processZippedXmlFile(xmlstuff, listener, progressbar);
		long endParsingTimeStamp = System.currentTimeMillis();

		Log.d(TAG, "Retrieve and unzip XML file: " + (startParsingTimeStamp - startReadingTimeStamp) + "mSec");
		Log.d(TAG, "Parse XML file and load into DB: " + (endParsingTimeStamp - startParsingTimeStamp) + "mSec");
	}
}
