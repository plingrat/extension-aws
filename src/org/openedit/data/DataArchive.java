package org.openedit.data;

import java.util.Collection;

import org.openedit.Data;
import org.openedit.xml.XmlArchive;

import com.openedit.users.User;

public interface DataArchive
{

	void setXmlArchive(XmlArchive inXmlArchive);

	void setDataFileName(String inDataFileName);

	void setElementName(String inSearchType);

	void setPathToData(String inPathToData);

	XmlArchive getXmlArchive();

	void delete(Data inData, User inUser);

	void saveData(Data inData, User inUser);

	void saveAllData(Collection<Data> inAll, User inUser);


}