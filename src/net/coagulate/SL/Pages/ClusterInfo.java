package net.coagulate.SL.Pages;

import net.coagulate.SL.HTTPPipelines.PageMapper.Url;
import net.coagulate.SL.HTTPPipelines.StringHandler;

/**
 *
 * @author Iain Price
 */
public class ClusterInfo extends StringHandler {

    @Url("/Info")
    public ClusterInfo(){super();}
    @Override
    protected String handleString() {
        return "<p align=center><table><tr><td style=\"max-width: 800px\";>"
                + "<h1>Architecture Generations</h1>"
                + "<p>For the latest generation, please see <a href=\"#generation3\">Generation Three</a>.  The rest of this document is purely historical record, and mostly tracks the evolution of a Rental Box piece of software.</p>"
                + "<h1 name=\"generation0\">Generation Zero</h1>"
                + "<p>The retroactively named Generation Zero comprises the original Second Life solutions, before any internet resources were involved</p>"
                + "<p>That is, these solutions are stand alone Second Life constructions that only interact within Second Life, the most traditional solution</p>"
                + "<p>Problems with this are mainly with management at scale, or scale of features</p>"
                + "<p><b>Pros:</b> Simple and reliable, self contained entities that continue to run, if well programmed, as long as the local sim does</p>"
                + "<p><b>Cons:</b> Management of large numbers of distributed resources becomes complicated ; inter region comms must be set up in second life and objects must parse and process this information</p>"
                + "<h1 name=\"generation1\">Generation One</h1>"
                + "<p>Generation One solves the main issues with Generation Zero without fundamentally changing the reliability of the solution</p>"
                + "<p>Products remain roughly the same, self contained objects that run stand-alone, but utilise an internet host/database for reporting/control, and nothing more.</p>"
                + "<p><b>Pros:</b> All the reliability and simplicity of Generation Zero, without the management overheads.  Solution is resilient to server failure as it's only used for offloading data.</p>"
                + "<p><b>Cons:</b> All the usual Second Life Scripting gotchas</p>"                
                + "<h1 name=\"generation2\">Generation Two</h1>"
                + "<p>Generation Two is fundamentally different to Generation One and is designed to solve the remaining large 'con' of Generation One</p>"
                + "<p>This problem is one of complexity and limited resources in Second Life - e.g. a Rental box that has Skyboxes, texturable, security lists, user lists, prim return, bot automation, etc etc, will very quickly exceed the limits of what can be placed in one script.  Functionality can be split across multiple scripts, to an extent, but this incurs a performance penalty due to inter-script message passing, and incurs further overheads for the communication and control protocols necessary once this gets large</P>"
                + "<p>It was the desire to rewrite the parcel security for the rental box that made me abandon the Generation One design; instead Generation Two products offload most of their \"business logic\" to an external server.  In the case of the Rental Box, it was designed to be a one-script thin client that is literally instructed, call by call, what to do in world.  With other products I created a more structured set of interactions, with the SL components being the user interface for the server side logic.  This frees me almost entirely from the above constraints, grants increased power and performance.</P>"
                + "<p>The biggest concern with Generation Two solutions is the server's stability - servers need rebooting for patching, or may lose internet or otherwise die, it's necessary to create a pair of servers, and to this end I used mysql's built in row level asynchronous replication, which creates various caveats about parallel processes</p>"
                + "<p><b>Pros:</b> Dramatically increased capabilites for configuration, control, performance, storage, features etc</p>"
                + "<p><b>Cons:</b> Dependant on external pair of servers (never had both down in several years of use).  Asynchronous replication requires careful coding</p>"
                + "<h1 name=\"generation3\">Generation Three</h1>"
                + "<p>Much as Generation One was a refinement of Generation Zero, the third and final generation is an iteration of Generation Two</p>"
                + "<p>This generation resolves the asynchronous replication issue, and cleans up some implementation details around replication (i.e. having to initialise or reinitialise replicas)<p>"
                + "<p>At the heart of Generation Three is MariaDB with Galera clustering, this provides a synchronously replicating database, allowing data to be consistently written across all nodes, rather than the previous 'will apply when I can' approach of Generation Two</P>"
                + "<p>This also means cluster wide locking can occur which resolves some issues around making sure things don't happen twice on different servers / at the same time with other conflicting processes</p>"
                + "<p>Due to the nature of this synchronous replication, three servers are required.  With asynchronous, each server simply queues up commands, and the other plays them back as and when it can, with no guarantees of timing or ordering with its own transactions.  For safe synchronous replication all nodes must reach all other nodes in real time to co-ordinate - this creates issues with a net-split as neither node knows if its the live or dead one, and thus a 3 node solution is used, where any 2 or more nodes form the database cluster.  It is possible to reduce it to one node through controlled shutdown, or start a singular node in emergencies or for recovery, but the replication mechanism assures 'consensus' by reaching a majority (2) of the (3) nodes.</P>"
                + "<p>On top of this database sits an apache server and a custom Java solution which exposes HTML content for user interactions, and various API endpoints for LSL, usually in JSON over HTTP (with timestamped digest authentication)</p>"
                + "<p>These three nodes allow solving of the problems in Generation Two in not being able to co-ordinate tasks (e.g. billing, dont want to be billed twice for something) and ensure updates are visible across all nodes instantly, rather than 'some time soon after, hopefully'</p>"
                + "<p>At this point I have migrated ALL my previous services over to the Generation Three cluster and retired the previous solutions ; it's roughly backwards compatible with generation two solutions (the possibility of transaction rollback would only highlight design flaws in the original code), and can even host Generation One services, though they don't particularly benefit from this level of design or resilience.  I've started to re-engineer and tightly integrate previous services to maximally unify everything under a single 'Third Generation SL Networked Solution' (or fourth, if you like)</p>"
                + "<p><b>Pros:</b> Resilient, consistent, synchronous database cluster with Object Orientated programming driving LSL frontends; able to solve all previous generations requirements, and improve on them</p>"
                + "<p><b>Cons:</b> Requires three servers.  Still requires some coding knowledge to avoid problems with parallelism (though even generation zero may not avoid this, it's inherent to multi user environments)</p>"
                + "</td></tr></table></p>";
    }
    
}
