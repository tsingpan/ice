// **********************************************************************
//
// Copyright (c) 2001
// Mutable Realms, Inc.
// Huntsville, AL, USA
//
// All Rights Reserved
//
// **********************************************************************

#ifndef ICE_PACK_COMPONENT_DEPLOYER_H
#define ICE_PACK_COMPONENT_DEPLOYER_H

#include <IceUtil/Shared.h>
#include <IcePack/Admin.h>
#include <Yellow/Yellow.h>
#include <sax/HandlerBase.hpp>

#include <map>
#include <vector>
#include <stack>

namespace IcePack
{

class Task : public ::IceUtil::SimpleShared
{
public:

    virtual void deploy() = 0;
    virtual void undeploy() = 0;
};

// TODO: ML: Nonportable. The incRef/decRef declarations must be done
// before the typedef below. Why not just use IceUtil::Handle in this
// case?
typedef ::IceInternal::Handle< ::IcePack::Task> TaskPtr;

void incRef(::IcePack::Task*);
void decRef(::IcePack::Task*);

class ComponentDeployer;

class DeploySAXParseException : public SAXParseException
{
public:

    // TODO: ML: Space is missing: "const Locator* const
    // locator". Also, why not just const Locator*? What's the point
    // of making the pointer constant as well?
    DeploySAXParseException(const std::string&, const Locator*const locator);

};

//
// A wrapper for ParserDeploymentException.
//
class ParserDeploymentWrapperException : public SAXException
{
public:

    ParserDeploymentWrapperException(const ParserDeploymentException&);
    void throwParserDeploymentException() const;

private:

    ParserDeploymentException _exception;
};

class ComponentErrorHandler : public ErrorHandler
{
public:

    ComponentErrorHandler(ComponentDeployer&);

    void warning(const SAXParseException& exception);
    void error(const SAXParseException& exception);
    void fatalError(const SAXParseException& exception);
    void resetErrors();

private:

    ComponentDeployer& _deployer;
};

class ComponentDeployHandler : public DocumentHandler
{
public:

    ComponentDeployHandler(ComponentDeployer&);

    // TODO: ML: Incorrect spacing, should be "const XMLCh*
    // const". Also, loose the last const -- there is no point in
    // forcing the pointer itself to be const. (Here and everywhere
    // else.)
    virtual void characters(const XMLCh *const, const unsigned int);
    // TODO: ML: AttributeList&.
    virtual void startElement(const XMLCh *const, AttributeList &); 
    virtual void endElement(const XMLCh *const);

    // TODO: ML: No reason to make inline, see style guide.
    virtual void ignorableWhitespace(const XMLCh *const, const unsigned int) { }
    virtual void processingInstruction(const XMLCh *const, const XMLCh *const) { }
    virtual void resetDocument() { }
    virtual void setDocumentLocator(const Locator *const);
    virtual void startDocument() { }
    virtual void endDocument() { }

protected:

    std::string getAttributeValue(const AttributeList&, const std::string&) const;
    std::string getAttributeValueWithDefault(const AttributeList&, const std::string&, const std::string&) const;

    std::string toString(const XMLCh *const) const;
    std::string elementValue() const;

private:

    std::stack<std::string> _elements;
    std::string _adapter;

    const Locator* _locator;
    ComponentDeployer& _deployer;
};

class ComponentDeployer : public Task
{
public:

    ComponentDeployer(const Ice::CommunicatorPtr&);

    virtual void deploy();
    virtual void undeploy();

    void parse(const std::string&, ComponentDeployHandler&);
    void setDocumentLocator(const Locator*const locator);
    std::string substitute(const std::string&) const;

    void createDirectory(const std::string&, bool = false);
    void createConfigFile(const std::string&);
    void addProperty(const std::string&, const std::string&);
    void addOffer(const std::string&, const std::string&, const std::string&);
    void overrideBaseDir(const std::string&);

protected:

    void undeployFrom(std::vector<TaskPtr>::iterator);

    Ice::CommunicatorPtr _communicator;
    Yellow::AdminPrx _yellowAdmin;

    Ice::PropertiesPtr _properties;
    std::map<std::string, std::string> _variables;
    std::vector<TaskPtr> _tasks;
    std::string _configFile;

    const Locator* _locator;
};

}

#endif
