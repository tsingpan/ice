// **********************************************************************
//
// Copyright (c) 2003-2005 ZeroC, Inc. All rights reserved.
//
// This copy of Ice is licensed to you under the terms described in the
// ICE_LICENSE file included in this distribution.
//
// **********************************************************************

package IceInternal;

public final class TraceLevels
{
    TraceLevels(Ice.Properties properties)
    {
        networkCat = "Network";
        protocolCat = "Protocol";
        retryCat = "Retry";
        securityCat = "Security";
	locationCat = "Location";
	slicingCat = "Slicing";

        final String keyBase = "Ice.Trace.";

        network = properties.getPropertyAsInt(keyBase + networkCat);
	protocol = properties.getPropertyAsInt(keyBase + protocolCat);
        retry = properties.getPropertyAsInt(keyBase + retryCat);
        security = properties.getPropertyAsInt(keyBase + securityCat);
	location = properties.getPropertyAsInt(keyBase + locationCat);
	slicing = properties.getPropertyAsInt(keyBase + slicingCat);
    }

    final public int network;
    final public String networkCat;
    final public int protocol;
    final public String protocolCat;
    final public int retry;
    final public String retryCat;
    final public int security;
    final public String securityCat;
    final public int location;
    final public String locationCat;
    final public int slicing;
    final public String slicingCat;
}
