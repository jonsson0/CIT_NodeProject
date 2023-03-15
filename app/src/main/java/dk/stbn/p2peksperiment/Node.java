package dk.stbn.p2peksperiment;

import java.util.List;

public class Node {
    List<String> phoneBookLeft;
    List<String> phoneBookRight;

    Data dataOwn = new Data();

    Neighbor neighborLeft = new Neighbor();

    Neighbor neighborRight = new Neighbor();

    String Id;

    List<String> DataIndex;
    List<String> Data;


    Node(String ip, List<String> nodesLeft, List<String> nodesRight){
        Id = ip;
        phoneBookLeft = nodesLeft;
        phoneBookRight = nodesRight;
    }


    String getId(){
        return Id;
    }

    List<String> newNeighbor(List<String> newNeighborIds, String side){
        if(side.equals("left")){
            phoneBookLeft.addAll(newNeighborIds);
            return phoneBookLeft;
        }else{
            phoneBookRight.addAll(newNeighborIds);
            return phoneBookRight;
        }
    }

    List<String> GetPhonebookLeft(){
        return phoneBookLeft; //returns nodes left or right;
    }

    List<String> GetPhonebookRight(){
        return phoneBookRight; //returns nodes left or right;
    }

    String GetData(String dataId){
        if(DataIndex.contains(dataId)) {
            for (int i = 0; i < DataIndex.size(); i++) {
                if (dataId.equals(DataIndex.get(i))) {
                    return Data.get(i);
                }
            }
        }else{
            return null; //TODO logic for sending get data to a new node left or right
        }
        return null;
    }

    void RemoveData(String dataId){
        for (int i = 0; i < DataIndex.size(); i++) {
            if(dataId.equals(DataIndex.get(i))){
                Data.remove(i);
                DataIndex.remove(i);
                break;
            }
        }
    }

    String AddData(String newData){
        Data.add(newData);
        //String hashedNewData = hash(newdata)
        DataIndex.add(newData);//this should be hashedNewData
        return "datas index"; //should return hashedNewData
        //TODO fix with hash
    }

}
