package com.example.mrhan.maketravel;

/**
 * Created by Mr.Han on 2018/5/23.
 */

import java.util.*;



public class MyAlgorithm {
    public static void main(String[] args)
    {
    }

    public MyAlgorithm(String city)
    {
        db = new TravelDB(city);
        all_scene = db.getAllScene();
    }

    //数据库类
    public TravelDB db;

    //储存所有景点ID
    private List<String> all_scene;
    //储存已有各个类型景点数目
    private Map<String, Integer> kindNumber = new HashMap<String, Integer>();
    //储存已有景点ID
    public List<String> chosenScene = new ArrayList<String>();
    //景点-评分（按评分由高到低排序）
    private List<Map.Entry<String, Double>> sceneScore;

    //用户兴趣列表
    private Set<String> interests = new HashSet<String>();
    //判断是否需要重新计算分数
    public boolean refresh = true;
    //出行方式
    private String transportation = "taxi";
    public void setTransportation(String trans)
    {
        transportation = trans;
    }

    //将一个景点加入已有景点列表
    public void addScene(String ID)
    {
        chosenScene.add(ID);
        //对已有景点类型列表修改
        String scene_type = db.getType(ID);
        Integer old_num = kindNumber.get(scene_type);
        kindNumber.put(scene_type, old_num==null?1:old_num+1);
        //修改标识
        refresh = true;
    }

    //将一个景点从已有景点列表中删除
    public void removeScene(String ID)
    {
        chosenScene.remove(ID);
        //对已有景点类型列表修改
        String scene_type = db.getType(ID);
        Integer old_num = kindNumber.get(scene_type);
        kindNumber.put(scene_type, old_num-1);
        //修改标识
        refresh = true;
    }

    //将用户兴趣加入列表
    public void addInterest(String interest)
    {
        interests.add(interest);
        refresh = true;
    }

    //将用户兴趣移出列表
    public void removeInterest(String interest)
    {
        interests.remove(interest);
        refresh = true;
    }


    //检测一个景点是否为已选景点的相邻景点
    private boolean isNeighbor(String ID, double distance_limit)
    {
        Iterator<String> it = chosenScene.iterator();
        while(it.hasNext())
        {
            if(db.getDistance(it.next(), ID, "taxi") < distance_limit)
                return true;
        }
        return false;
    }

    //返回推荐列表
    public List<String> getRecommandScene()
    {
        //兴趣因子
        Double interests_parameter = 0.5;
        //重复因子
        Double repeat_parameter = 0.25;

        //储存评分最高的景点
        int top_number = 2;
        List<String> top_scene = new ArrayList<String>();

        //储存距离已有的景点评分最高的景点
        int neighbor_number = 2;
        List<String> neighbor_scene = new ArrayList<String>();


        //需重新计算景点评分表
        if(refresh)
        {
            //暂存ID-评分键值对
            Map<String, Double> tmp = new HashMap<String, Double>();
            //所有景点的List
            Iterator<String> it1 = all_scene.iterator();
            while(it1.hasNext())
            {
                String now_ID = it1.next();
                //已经被选中
                if(chosenScene.contains(now_ID)) continue;

                //Pop评分
                Double score = db.getPop(now_ID);
                //兴趣评分
                if(interests.contains(db.getType(now_ID))) score += interests_parameter;
                //重复程度评分
                Integer same_kind_scene = kindNumber.get(db.getType(now_ID));
                same_kind_scene = same_kind_scene==null?0:same_kind_scene;
                score -= repeat_parameter*((double)same_kind_scene);

                //加入键值对
                tmp.put(now_ID, score);
            }
            sceneScore = new ArrayList(tmp.entrySet());
            Collections.sort(sceneScore, new Comparator<Map.Entry<String, Double>>() {
                public int compare(Map.Entry<String,Double> o1, Map.Entry<String,Double> o2) {
                    return (o2.getValue()>o1.getValue())?1:-1;
                } } );

            refresh = false;
        }
        /*
        //根据景点评分表给出推荐
        Iterator<Map.Entry<String, Double>> it = sceneScore.iterator();
        while(it.hasNext() && (top_scene.size()<top_number || (!chosenScene.isEmpty() && neighbor_scene.size()<neighbor_number)))
        {
            String now_ID = it.next().getKey();
            //加入到邻近推荐列表
            if(neighbor_scene.size()<neighbor_number && isNeighbor(now_ID,500)) //坐出租车500s
            {
                neighbor_scene.add(now_ID);
                it.remove();
            }
            //加入到高评分推荐列表
            else if(top_scene.size() < top_number)
            {
                top_scene.add(now_ID);
                it.remove();
            }
        }

        //生成推荐列表
        top_scene.addAll(neighbor_scene);
        if(top_scene.size()<4)
        {
            Iterator<Map.Entry<String, Double>> it2 = sceneScore.iterator();
            while(it2.hasNext() && top_scene.size()<4)
            {
                top_scene.add(it.next().getKey());
                it.remove();
            }
        }
        */
        Iterator<Map.Entry<String, Double>> it3 = sceneScore.iterator();
        while(it3.hasNext()) {top_scene.add(it3.next().getKey());}
        return top_scene;
    }

    //推荐酒店
    public List<String> getRecommendHotel(String need)
    {
        List<String> tmp = new ArrayList<String>(db.getAllHotel());

        /*
        List<String> res = new ArrayList<String>();

        System.out.println(tmp);

        //距离景点足够近
        for(String hotel: tmp)
            if(isNeighbor(hotel, 500)) //坐taxi 100秒
                res.add(hotel);

        //排序方式
        //按价格
        if(need.equals("cheap"))
        {
            Collections.sort(res, new Comparator<String>() {
                public int compare(String o1, String o2) {
                    return (int) (db.getPrice(o2)-db.getPrice(o1));
                } } );
        }
        //按评分
        else if(need.equals("popular"))
        {
            Collections.sort(res, new Comparator<String>() {
                public int compare(String o1, String o2) {
                    return (int) (db.getPop(o2)-db.getPop(o1));
                } } );
        }

        //综合
        else
        {
            final double price_parameter = 25.0;
            Collections.sort(res, new Comparator<String>() {
                public int compare(String o1, String o2) {
                    return ((db.getPop(o2)+price_parameter/db.getPrice(o2)>db.getPop(o1)+price_parameter/db.getPrice(o1)?1:-1));
                } } );
        }
        */
        return tmp;
    }


    //获得大约的时间和价格 0 时间 1 价格
    public List<String> getTimeAndPrice()
    {

        //0是时间 1是价格
        double [] tmp_list = new double[2];
        tmp_list[0] = 0;
        tmp_list[1] = 0;
        List<String> res = new ArrayList<String>();
        if(chosenScene.size()==0) return null;

        //储存最优排序
        int []best_choice = new int[chosenScene.size()];
        double best_time = 100000.0;
        double best_price = 0.0;
        //储存当前排序
        int []now_choice = new int[chosenScene.size()];
        double now_time =  0.0;
        double now_price = 0.0;

        //各景点访问时间
        double visit_time = 0.0;
        //组合的总可能性
        int total_possibility = 1;
        //初始化价格总数，距离矩阵，选择数组
        for(int i=0; i<chosenScene.size(); ++i)
        {
            visit_time += db.getVisitTime(chosenScene.get(i));
            now_choice[i] = i;
            total_possibility *= (i+1);
            tmp_list[1] += db.getPrice(chosenScene.get(i));
        }

        //暴力找用时最少的
        for(int i=0; i<total_possibility; ++i)
        {
            now_time = db.getDistance(chosenScene.get(now_choice[chosenScene.size()-1]),chosenScene.get(now_choice[0]), transportation);
            for(int j=1; j<chosenScene.size() && now_time < best_time; ++j)
            {
                now_time += db.getDistance(chosenScene.get(now_choice[j-1]),chosenScene.get(now_choice[j]), transportation);
                now_price += db.getTransportPrice(chosenScene.get(now_choice[j-1]),chosenScene.get(now_choice[j]), transportation);
            }

            if(now_time < best_time)
            {
                best_time = now_time;
                for(int j=0; j<chosenScene.size(); ++j)
                    best_choice[j] = now_choice[j];
                best_price = now_price;
            }
            nextPermutation(now_choice);
        }
        tmp_list[0] = visit_time + best_time;
        tmp_list[1] += best_price;
        res.add(get_time_duration(tmp_list[0]));
        res.add(Double.toString(tmp_list[1])+"元");
        return res;
    }

    //计算用工具类
    private void nextPermutation(int[] num) {
        if(num==null || num.length==0)
            return;
        int i = num.length-2;
        while(i>=0 && num[i]>=num[i+1])
            i--;

        if(i>=0){
            int j=i+1;
            while(j<num.length && num[j]>num[i])
                j++;
            j--;
            swap(num,i,j);
        }
        reverse(num, i+1,num.length-1);
    }
    private void swap(int[] num, int i, int j){
        int tmp = num[i];
        num[i] = num[j];
        num[j] = tmp;
    }
    private void reverse(int[] num, int i, int j){
        while(i < j)
            swap(num, i++, j--);
    }


    //决定酒店
    String hotelID;
    public void decideHotel(String id)
    {
        hotelID = id;
    }

    //给出路线 ids 0 总天数， 1 总费用, 2 交通方式 其余各景点Id
    public List<String> getRoute()
    {

        //总花销
        double cost = db.getPrice(hotelID);
        //储存最优排序
        int []best_choice = new int[chosenScene.size()];
        double best_time = 100000.0;
        double best_day = 1000.0;
        double best_price = 0.0;
        //储存当前排序
        int []now_choice = new int[chosenScene.size()];
        //组合的总可能性
        int total_possibility = 1;
        //初始化组合顺序，解空间大小，门票总花销
        for(int i=0; i<chosenScene.size(); ++i)
        {
            now_choice[i] = i;
            total_possibility *= (i+1);
            cost += db.getPrice(chosenScene.get(i));
        }


        double lunch_time = 5400; //5400s午餐
        //暴力找用时最少的
        for(int i=0; i<total_possibility; ++i)
        {
            double now_total_time =  0.0;
            int now_day = 1;
            double today_time = 0.0;
            boolean need_lunch = true;
            Double now_price = 0.0;

            for(int j=0; j<chosenScene.size(); ++j)
            {
                //新的一天开始
                if(today_time == 0)
                {
                    today_time += db.getDistance(hotelID,chosenScene.get(now_choice[j]), transportation);
                    now_price += db.getTransportPrice(hotelID,chosenScene.get(now_choice[j]), transportation);
                    today_time += db.getVisitTime(chosenScene.get(now_choice[j]));
                    need_lunch = true;
                    continue;
                }

                today_time += db.getDistance(chosenScene.get(now_choice[j-1]), chosenScene.get(now_choice[j]), transportation);
                today_time += db.getVisitTime(chosenScene.get(now_choice[j]));

                //检测是否到午饭时间
                if(need_lunch && today_time > 10800) //10800s = 3h = 11:00后吃饭
                {
                    need_lunch = false;
                    today_time  += lunch_time;
                }

                //该天结束
                if(today_time > 32400) //32400s = 9h = 17:00
                {
                    now_total_time += today_time + db.getDistance(chosenScene.get(now_choice[j]),hotelID, transportation);
                    now_price += db.getTransportPrice(chosenScene.get(now_choice[j]),hotelID, transportation);
                    today_time = 0;
                    now_day += 1;
                    if(today_time > best_time || now_day > best_day)
                        break;
                }
            }
            now_total_time += today_time;

            if(now_day < best_day || (now_day==best_day && now_total_time <best_time))
            {
                for(int j=0; j<chosenScene.size(); ++j)
                    best_choice[j] = now_choice[j];
                best_time = now_total_time;
                best_day = now_day;
                best_price = now_price;
            }


            nextPermutation(now_choice);
        }

        cost += best_price;
        cost += best_day * db.getPrice(hotelID);

        //需要返回行程顺序（包括午餐），总时间，总费用， 若要修改路线实现想法：传入一个数组，分别对应每个景点访问时间时间
        List<String> res = new ArrayList<String>();
        res.add(Double.toString(best_day)); res.add(Double.toString(cost));
        res.add(transportation);


        for(int i=0; i<chosenScene.size(); ++i)
            res.add(chosenScene.get(best_choice[i]));

        System.out.println(res);
        return res;
    }


    //输入经过的秒数，输出点钟
    public String get_clock(Double time)
    {
        String res;
        int hour = 8 + (int) (time/3600);
        time = time % 3600;
        int minute = (int) (time/60);
        res = Integer.toString(hour) + ":";
        if(minute<10) res += "0" + Integer.toString(minute);
        else res += Integer.toString(minute);
        return res;
    }

    //输入经过的秒数，输出时长
    public String get_time_duration(Double time)
    {
        String res;
        int hour = (int) (time/3600);
        time = time % 3600;
        int minute = (int) (time/60);
        res = Integer.toString(hour) + "小时" + Integer.toString(minute)+"分";
        return res;
    }

    //ids 0 总天数， 1 总费用, 2 交通方式 其余各景点Id
    public List<String> get_route_in_line(List<String> ids)
    {
        double lunch_time = 5400;
        List<String> res = new ArrayList<String>();
        List<String> formap; //暂时没用，为了显示地图可能有用

        int now_day = 1;
        double today_time = 0.0;
        boolean need_lunch = true;
        for(int j=0; j<chosenScene.size(); ++j)
        {

            //新的一天开始
            if(today_time == 0)
            {
                res.add("Day"+now_day);
                double tmp = db.getDistance(hotelID,ids.get(j+3), ids.get(2));
                String trans = transportation.equals("taxi")?"乘出租车":"乘公交车";
                if(tmp<240) {tmp*=5; trans="步行";}
                res.add("从"+hotelID+"出发,"+ trans + "前往"+ids.get(j+3)+",历时"+get_time_duration(tmp) + "    "
                        +get_clock(today_time) + "——" + get_clock(today_time += tmp));

                res.add( "游览" + ids.get(j+3) + ",历时"+get_time_duration(db.getVisitTime(ids.get(j+3)))+"    "
                        +get_clock(today_time) + "——" + get_clock(today_time+= db.getVisitTime(ids.get(j+3))));

                need_lunch = true;
                continue;
            }
            System.out.println(today_time);


            double tmp = db.getDistance(ids.get(j+2),ids.get(j+3), ids.get(2));
            String trans = transportation.equals("taxi")?"乘出租车":"乘公交车";
            if(tmp<240) {tmp*=5; trans="步行";}

            //检测是否到午饭时间
            if(need_lunch && today_time > 10800) //10800s = 3h = 11:00后吃饭
            {
                res.add("建议午餐时间,历时"+get_time_duration(5400.0)+"    "+get_clock(today_time) + "——" + get_clock(today_time  += lunch_time));
                need_lunch = false;
            }
            res.add("从"+ids.get(j+2)+"出发，"+trans+"前往"+ids.get(j+3)+",历时"+get_time_duration(tmp) + "    "
                    +get_clock(today_time) + "——" + get_clock(today_time += tmp));

            //检测是否到午饭时间
            if(need_lunch && today_time > 10800) //10800s = 3h = 11:00后吃饭
            {
                res.add("建议午餐时间,历时"+get_time_duration(5400.0)+"    "+get_clock(today_time) + "——" + get_clock(today_time  += lunch_time));
                need_lunch = false;
            }
            res.add( "游览" + ids.get(j+3) + ",历时"+get_time_duration(db.getVisitTime(ids.get(j+3)))+"    "
                    +get_clock(today_time) + "——" + get_clock(today_time+= db.getVisitTime(ids.get(j+3))));
            //检测是否到午饭时间
            if(need_lunch && today_time > 10800) //10800s = 3h = 11:00后吃饭
            {
                res.add("建议午餐时间,历时"+get_time_duration(5400.0)+"    "+get_clock(today_time) + "——" + get_clock(today_time  += lunch_time));
                need_lunch = false;
            }

            //该天结束
            if(today_time > 32400) //32400s = 9h = 17:00
            {

                double tmp1 = db.getDistance(ids.get(j+3),hotelID, ids.get(2));
                String trans1 = transportation.equals("taxi")?"乘出租车":"乘公交车";
                if(tmp<240) {tmp*=5; trans1="步行";}
                res.add("从"+ids.get(j+3)+trans1+"返回"+ hotelID +",历时"+get_time_duration(tmp1) + "    "
                        +get_clock(today_time) + "——" + get_clock(today_time += tmp1));

                today_time = 0;
                now_day += 1;
            }
        }
        res.add("结束本次旅行");
        return res;

    }

}