package tk.rabidbeaver.mcuradio;

public class Channel {
    public int frequency, priority;
    public boolean fm;
    public Channel(int frequency, boolean fm, int priority){
        this.frequency = frequency;
        this.priority = priority;
        this.fm = fm;
    }
}
