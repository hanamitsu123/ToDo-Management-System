package com.dmm.task.controller;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.dmm.task.data.entity.Tasks;
import com.dmm.task.data.repository.TasksRepository;
import com.dmm.task.form.TaskForm;
import com.dmm.task.service.AccountUserDetails;

@Controller
public class TodoController {
	
	@Autowired
	private TasksRepository repo;
	
	//ログイン画面への遷移
	@GetMapping("/loginForm")
		String loginForm() {
			
			//login.htmlへ遷移
			return "login";
		}
	
	//カレンダー表示用	
	@GetMapping("/main")
	public String main(Model model,@AuthenticationPrincipal AccountUserDetails user,
	@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
		
		// 週と日を格納する二次元のListを用意する
	    List<List<LocalDate>> month = new ArrayList<>();

	    // 1週間分のLocalDateを格納するListを用意する
	    List<LocalDate> week = new ArrayList<>();

	    // 日にちを格納する変数を用意する
	    LocalDate day, start, end; 
	    
	    //今月 or 前月 or 翌月を判定する
	    if(date == null) {
	    
	    	// その月の1日を取得する
	    	day = LocalDate.now();  // 現在日時を取得
	    	day = LocalDate.of(day.getYear(), day.getMonthValue(), 1);  // 現在日時からその月の1日を取得
	    
	    }else {
	        day = date;  // 引数で受け取った日付をそのまま使う
	    }
	    
	    //カレンダーの ToDo直下に「yyyy年mm月」と表示
	    model.addAttribute("month", day.format(DateTimeFormatter.ofPattern("yyyy年MM月")));
	    
	    // 翌月のリンク
	    model.addAttribute("next", day.plusMonths(1));
	    
	    // 前月のリンク
	    model.addAttribute("prev", day.minusMonths(1));
	    
	    // 前月分の LocalDateを求める
	    DayOfWeek w = day.getDayOfWeek();  // 当該日の曜日を取得
	    day = day.minusDays(w.getValue());  // 1日からマイナスして 4/28を取得
	    start = day; 
	    
	    // 1週目（1日ずつ増やして 週のリストに格納していく）
	    for(int i = 1; i <= 7; i++) {
	    	week.add(day);  // 週のリストへ格納
	    	day = day.plusDays(1);  // 1日進める
	    }    
	    
	    month.add(week);  // 1週目のリストを、月のリストへ格納する

	    week = new ArrayList<>();  // 次週のリストを新しくつくる
	    
	    // 2週目（2週目から月末まで）
	    int leftOfMonth = day.lengthOfMonth() - day.getDayOfMonth();    // ①1週目の当月分
	    leftOfMonth = day.lengthOfMonth() - leftOfMonth;    // ②当月の全日数から①を引く
	    leftOfMonth = 7 - leftOfMonth;    // ③int i = 7を考慮した追加日数

	    for(int i = 7; i < day.lengthOfMonth() + leftOfMonth; i++) {    // day.lengthOfMonth() に③を加える
	        w = day.getDayOfWeek();
	        week.add(day);
	        if(w == DayOfWeek.SATURDAY){
	            month.add(week);   // 月に週を追加
	            week = new ArrayList<>();  // 新しい週のListを作成
	        }
	            
	        day = day.plusDays(1);
	        System.out.println(day);    // [DEBUG] ここで当月の末日が出力されること
	    }
		    
	    // 最終週の翌月分
	    w = day.getDayOfWeek();
	    int remainDays = 7 - w.getValue();
	    if(7 - w.getValue() == 0) {
	    	remainDays = 7;
	    }
	    for(int i = 1; i < remainDays; i++) {  
	    	week.add(day);
	    	day = day.plusDays(1);
	    }
	    
	    end = day;
	    
	    week.add(day);	
		month.add(week);
	    
	    // 日付とタスクを紐付けるコレクション
	    MultiValueMap<LocalDate, Tasks> tasks = new LinkedMultiValueMap<LocalDate, Tasks>();
	    
	    // リポジトリからタスクを取得
	    List<Tasks> list;  
	    
	    if(user.getUsername().equals("admin")) {
	    	 // 管理者だったら
	    	list = repo.findAllByDateBetween(start.atTime(0,0), end.atTime(0,0));
	    } else {  
	    	// ユーザーだったら
	    	list = repo.findByDateBetween(start.atTime(0, 0),end.atTime(0, 0), user.getName());	      
	    }
	    // 取得したタスクをコレクションに追加
	    for(Tasks task : list) {
	      tasks.add(task.getDate().toLocalDate(), task);
	    }
	  
	    // カレンダーのデータをHTMLに連携
	    model.addAttribute("matrix", month); 
	    
	    
	    // コレクションのデータをHTMLに連携
	    model.addAttribute("tasks", tasks);
	    
	    //main.htmlへ遷移
	    return "main";
	}
	
	// タスク登録画面の表示用
	@GetMapping("/main/create/{date}")
	public String create(Model model, @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
		
		//create.htmlへ遷移
		 return "create";
	}

	//登録画面への遷移
	@GetMapping("create")
		String create() {
					
		//create.htmlへ遷移
		return "create";
	}
	
	// タスク登録用
	@PostMapping("/main/create")
	public String createPost(Model model, TaskForm form, @AuthenticationPrincipal AccountUserDetails user) {
		Tasks task = new Tasks();
	    task.setName(user.getName());
	    task.setTitle(form.getTitle());
	    task.setText(form.getText());
	    task.setDate(form.getDate().atTime(0, 0));

	    repo.save(task);
	    
	    return "redirect:/main";
	}
	
	// タスク編集画面の表示用
	@GetMapping("/main/edit/{id}")
	public String edit(Model model, @PathVariable Integer id) {
		  Tasks task = repo.getById(id);
		  model.addAttribute("task", task);
		  
		  // ecdit.htmlへ遷移
		  return "edit";
	}
	  
	// タスク編集用
	@PostMapping("/main/edit/{id}")
	public String editPost(Model model, TaskForm form, @PathVariable Integer id, @AuthenticationPrincipal AccountUserDetails user) {
		  Tasks task = new Tasks();
		  task.setId(id);
		    
		  task.setName(user.getName());
		  task.setTitle(form.getTitle());
		  task.setText(form.getText());
		  task.setDate(form.getDate().atTime(0, 0));
		  task.setDone(form.isDone());

		  repo.save(task);

		  return "redirect:/main";
	  }
	  
	//タスク削除用
	@PostMapping("/main/delete/{id}")
	public String deletePost(Model model, TaskForm form, @PathVariable Integer id) {
		  Tasks task = new Tasks();
		  task.setId(id);

		  repo.delete(task);
		  
		  return "redirect:/main";
		}
}
