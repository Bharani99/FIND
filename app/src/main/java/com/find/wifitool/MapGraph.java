package com.find.wifitool;
import android.os.Parcel;
import android.os.Parcelable;

import org.jgrapht.UndirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

// The rooms and the sensors are managed as graphs (as mentioned in the IndoorGML standard)
// Each room (or sensor) is a State, and each state is connected with each other by a Transition
public class MapGraph
{
    // We need State as implementation of a parcelable object in order to provide it to the search
    // activity
    public static class State implements Parcelable
    {
        public String id;
        public String label;
        public float[] coords;

        public State(String id, String label, float x, float y)
        {
            this.id = id;
            this.label = label;
            this.coords = new float[2];
            this.coords[0] = x;
            this.coords[1] = y;
        }

        public State(){}

        public State(State state)
        {
            this.id = state.id;
            this.label = state.label;
            this.coords = new float[2];
            this.coords[0] = state.coords[0];
            this.coords[1] = state.coords[1];
        }

        // Parcelable methods
        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel out, int flags) {
            out.writeString(id);
            out.writeString(label);
            out.writeFloatArray(coords);
        }

        public static final Parcelable.Creator<State> CREATOR = new Creator<State>() {
            public State createFromParcel(Parcel in) {
                State state = new State();
                state.id = in.readString();
                state.label = in.readString();
                state.coords = in.createFloatArray();
                return state;
            }

            public State[] newArray(int size) {
                return new State[size];
            }
        };
    }
    
    public static class Transition
    {
    	public State start, end;
    	
    	public Transition(State start, State end)
    	{
    		this.start = start;
    		this.end = end;
    	}
    }


    private  UndirectedGraph<State, DefaultEdge> graph;
    private HashMap<String, State> vertixList;

    public  MapGraph(HashMap<String, State> vertixList, List<Transition> transList)
    {
    	graph = new SimpleGraph<>(DefaultEdge.class);
    	this.vertixList = vertixList;
    	
    	// add all vertices
        for(State s : vertixList.values())
        	graph.addVertex(s);
        
        // add all transitions
        for(Transition t : transList)
        	graph.addEdge(t.start, t.end);
        
    }
    
    public boolean contains(State vertex)
    {
    	return graph.containsVertex(vertex);
    }
    
    public State getState(String id)
    {
    	return vertixList.get(id);
    }

    public Set<State> getAllStates()
    {
        return graph.vertexSet();
    }

    public UndirectedGraph<State,DefaultEdge> getGraph()
    {
        return graph;
    }
}
