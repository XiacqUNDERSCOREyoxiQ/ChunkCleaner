package xiacq.chunkcleaner.core.utility;

public class ChunkCoordinates {
    private final  int X;
    private final int Z;

    public ChunkCoordinates(int x, int z) {
        this.X = x;
        this.Z = z;
    }

    public boolean equals(Object object) {
        if(!(object instanceof ChunkCoordinates otherObject))
            return false;
        return otherObject.X == this.X && otherObject.Z == this.Z;
    }

    public int hashCode() {return 31*this.X+this.Z;}
    public String toString() {return "{"+this.X + "|"+this.Z+"}";}

}
