// **********************************************************************
//
// Copyright (c) 2001
// Mutable Realms, Inc.
// Huntsville, AL, USA
//
// All Rights Reserved
//
// **********************************************************************

#include <Ice/Ice.h>
#include <Freeze/DB.h>
#include <Freeze/Evictor.h>
#include <Ice/Functional.h>
#include <Ice/LoggerUtil.h>
#include <IcePack/ServerManagerI.h>
#include <IcePack/AdapterManager.h>
#include <IcePack/Activator.h>
#include <IcePack/ServerDeployer.h>

using namespace std;
using namespace Ice;
using namespace IcePack;

namespace IcePack
{

class ServerNameToServer
{
public:

    ServerNameToServer(const ObjectAdapterPtr& adapter) :
	_adapter(adapter)
    {
    }

    ServerPrx
    operator()(const string& name)
    {
	Identity ident;
	ident.category = "server";
	ident.name = name;
	return ServerPrx::uncheckedCast(_adapter->createProxy(ident));
    }

private:

    ObjectAdapterPtr _adapter;
};

class ServerFactory : public ObjectFactory
{
public:

    ServerFactory(const ObjectAdapterPtr& adapter, const ActivatorPrx& activator) :
	_adapter(adapter),
	_activator(activator)
    {
    }

    virtual Ice::ObjectPtr 
    create(const std::string& type)
    {
	assert(type == "::IcePack::Server");
	return new ServerI(_adapter, _activator);
    }

    virtual void 
    destroy()
    {
	_adapter = 0;
	_activator = 0;
    }

private:
    
    ObjectAdapterPtr _adapter;
    ActivatorPrx _activator;
};

}

IcePack::ServerI::ServerI(const ObjectAdapterPtr& adapter, const ActivatorPrx& activator) :
    _adapter(adapter), 
    _activator(activator),
    _state(Inactive)
{
}

IcePack::ServerI::~ServerI()
{
}

ServerDescription
IcePack::ServerI::getServerDescription(const Current&)
{
    return _description;
}

bool
IcePack::ServerI::start(const Current&)
{
    while(true)
    {
	IceUtil::Monitor< IceUtil::Mutex>::Lock sync(*this);
	if(!_activator) // TODO: ML: { } missing, here and in several places below.
	    return false;

	switch(_state)
	{
	case Inactive:
	    _state = Activating;
	    break;

	case Activating:
	    wait(); // TODO: Timeout?
	    continue;

 	case Active:
	    return true; // Raise an exception instead?

	case Deactivating:
	    wait();
	    continue;

	case Destroyed:
	    throw ObjectNotExistException(__FILE__,__LINE__);
	}
	break;
    }

    try
    {
	bool activated = _activator->activate(ServerNameToServer(_adapter)(_description.name));
	setState(activated ? Active : Inactive);
	return activated;
    }
    catch (const SystemException& ex)
    {
	Warning out(_adapter->getCommunicator()->getLogger());
	out << "activation failed for server `" << _description.name << "':\n";
	out << ex;

	setState(Inactive);
	return false;
    }
}

void
IcePack::ServerI::terminationCallback(const Current&)
{
    //
    // Callback from the activator indicating that the server
    // stopped. Change state to deactivating while we mark the server
    // adapters as inactive.
    //
    setState(Deactivating);

    //
    // Mark each adapter as inactive. _adapters is immutable when
    // state == Deactivating.
    //
    for(Adapters::iterator p = _adapters.begin(); p != _adapters.end(); ++p)
    {
	(*p)->markAsInactive();
    }

    setState(Inactive);
}

ServerState
IcePack::ServerI::getState(const Current&)
{
    IceUtil::Monitor< ::IceUtil::Mutex>::Lock sync(*this);
    return _state;
}

void
IcePack::ServerI::setState(ServerState state, const Current&)
{
    IceUtil::Monitor< ::IceUtil::Mutex>::Lock sync(*this);

    if(state == Destroyed && (_state == Active || _state == Deactivating))
	throw ServerNotInactiveException();

    _state = state;

    notifyAll();
}

IcePack::ServerManagerI::ServerManagerI(const ObjectAdapterPtr& adapter,
					const Freeze::DBEnvironmentPtr& dbEnv,
					const AdapterManagerPrx& adapterManager,
					const ActivatorPrx& activator) :
    _adapter(adapter),
    _adapterManager(adapterManager),
    _activator(activator)
{
    ObjectFactoryPtr serverFactory = new ServerFactory(adapter, activator);
    adapter->getCommunicator()->addObjectFactory(serverFactory, "::IcePack::Server");

    Freeze::DBPtr dbServers = dbEnv->openDB("servers", true);
    _evictor = dbServers->createEvictor(Freeze::SaveUponEviction);
    _evictor->setSize(100);
    _adapter->addServantLocator(_evictor, "server");

    //
    // Cache the server names for getAll(). This will load all the
    // server objects at the begining and might cause slow startup.
    //
    Freeze::EvictorIteratorPtr p = _evictor->getIterator();
    while(p->hasNext())
    {
	ServerPrx s = ServerPrx::checkedCast(_adapter->createProxy(p->next()));
	assert(s);
	ServerDescription desc = s->getServerDescription();
	_serverNames.insert(desc.name);
    }
}

IcePack::ServerManagerI::~ServerManagerI()
{
}

ServerPrx
IcePack::ServerManagerI::create(const ServerDescription& desc, const Current&)
{
    IceUtil::Mutex::Lock sync(*this);

    ServerPrx server = ServerNameToServer(_adapter)(desc.name);
    try
    {
	server->ice_ping();
	throw ServerExistsException();
    }
    catch (const ObjectNotExistException&)
    {
    }
    
    ServerPtr serverI = new ServerI(_adapter, _activator);
    serverI->_description = desc;
    for(AdapterNames::const_iterator p = desc.adapters.begin(); p != desc.adapters.end(); ++p)
    {
	AdapterPrx adapter = _adapterManager->findByName(*p);
	serverI->_adapters.push_back(adapter);
    }

    _evictor->createObject(server->ice_getIdentity(), serverI);

    _serverNames.insert(desc.name);

    return server;
}

ServerPrx
IcePack::ServerManagerI::findByName(const string& name, const Current&)
{
    IceUtil::Mutex::Lock sync(*this);

    ServerPrx server = ServerNameToServer(_adapter)(name);
    try
    {
	server->ice_ping();
	return server;
    }
    catch(const ObjectNotExistException&)
    {
	return 0;
    }
}

void
IcePack::ServerManagerI::remove(const string& name, const Current&)
{
    IceUtil::Mutex::Lock sync(*this);

    ServerPrx server = ServerNameToServer(_adapter)(name);
    try
    {
	server->ice_ping();
    }
    catch(const ObjectNotExistException&)
    {
	throw ServerNotExistException();
    }

    _evictor->destroyObject(server->ice_getIdentity());

    _serverNames.erase(_serverNames.find(name));
}

ServerNames
IcePack::ServerManagerI::getAll(const Current&)
{
    IceUtil::Mutex::Lock sync(*this);

    ServerNames names;
    names.reserve(_serverNames.size());
    copy(_serverNames.begin(), _serverNames.end(), back_inserter(names));

    return names;
}

