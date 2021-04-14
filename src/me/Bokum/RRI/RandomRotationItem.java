package me.Bokum.RRI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class RandomRotationItem extends JavaPlugin{

	private List<String> settingPlayer = new ArrayList<String>();
	private String settingInvenTitle = "§0§l아이템 설정";
	private int lockItemSlot = 35;
	private String ms = "§f[ §eRRI §f] §7";
	private boolean isStart = false;
	private mySchedule mainSch = new mySchedule();
	private HashMap<String, List<Integer>> lastClickMap = new HashMap<String, List<Integer>>();
	public Plugin plugin;
	
	//로테이션 관련
	private List<ItemStack> rotationList = new ArrayList<ItemStack>();
	private int rotationIndex = -1;
	
	public void onEnable() {
		
		Bukkit.getServer().getLogger().info("무작위 아이템 변경 플러그인 로딩 완료");
		Bukkit.getServer().getPluginManager().registerEvents(new myEvent(), this);
	
		plugin = this;
	}
	
	public void onDisable() {
	
		Bukkit.getServer().getLogger().info("무작위 아이템 변경 플러그인 언로드됨");
		
	}
	
	public void setRotationItem(Player p) {
		Inventory settingInven = Bukkit.createInventory(null, 27, settingInvenTitle);
		for(ItemStack item : rotationList) {
			settingInven.addItem(item);
		}
		p.openInventory(settingInven);
		settingPlayer.add(p.getName());
	}
	
	private void applyRotationItemSetting(Player p, Inventory setInven) {
		rotationList.clear();
		
		int applyCnt = 0;
		
		for(ItemStack item : setInven.getContents()) {
			if(item != null && item.getType() != Material.AIR) {
				rotationList.add(item);
				applyCnt += 1;
			}
		}
		
		p.sendMessage(ms+applyCnt+"개의 아이템 적용 완료");
	}
	
	private void startRotation(int timeQuantum) {
		isStart = true;
		Bukkit.broadcastMessage(ms+"§a이제 아이템 로테이션이 적용됩니다.");
		
		mainSch.schId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
			public void run() {
				mySchedule tmpSch = new mySchedule();
				tmpSch.schTimer = 10;
				tmpSch.schId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
					public void run() {
						if(--tmpSch.schTimer > 0) {
							Bukkit.broadcastMessage(ms+"§a"+tmpSch.schTimer+"초§7 후 아이템이 변경됩니다.");
							sendSound(Sound.BLOCK_NOTE_PLING);
						}else {
							Bukkit.getScheduler().cancelTask(tmpSch.schId);
							rotateItem();
						}
					}
				}, 10l, 20l);
			}
		}, 0l, timeQuantum*20+200);
		
	}
	
	private void sendSound(Sound sound) {
		for(Player p : Bukkit.getOnlinePlayers()) {
			p.playSound(p.getLocation(), sound, 1.0f, 1.0f);
		}
	}
	
	private void rotateItem() {
		if(rotationList.size() >= 2) {
			int random = -1;
			do {
				random = getRandom(0, rotationList.size()-1);	
			} while(random == rotationIndex);
			
			rotationIndex = random;
			
			setLockItem();
			Bukkit.broadcastMessage(ms+"아이템이 변경됐습니다.");
			sendSound(Sound.BLOCK_NOTE_XYLOPHONE);
		} else {
			Bukkit.broadcastMessage(ms+"로테이션 시도 중 버그 발생, 로테이션 시스템을 중지합니다.");
			stopRotationItem(null);
		}
	}
	
	private void setLockItem() {
		for(Player p : Bukkit.getOnlinePlayers()) {
			applyItem(p);
		}
	}
	
	private void applyItem(Player p) {
		p.getInventory().setItem(lockItemSlot, null);
		Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
			public void run() {
				p.getInventory().setItem(lockItemSlot, rotationList.get(rotationIndex));		
			}
		}, 20l);	
	}
	
	private void stopRotationItem(Player p) {
		Bukkit.getScheduler().cancelTask(mainSch.schId);
		rotationIndex = -1;
		isStart = false;
		if(p != null)
			Bukkit.broadcastMessage(ms+p.getName()+" 님이 아이템 로테이션을 중지하였습니다.");
	}
	
	//min ~ max 중 값 1개 반환
	public static int getRandom(int min, int max) {
		return (int)(Math.random() * (max - min + 1) + min);
	}
	
	//이 아래 부터는 임시 클래스들
	private class mySchedule{
		public int schId;
		public int schTimer;
	}
	
	//이벤트
	private class myEvent implements Listener{
		
		@EventHandler
		public void onCloseInventory(InventoryCloseEvent e) {
			if(e.getPlayer() instanceof Player) {
				Player p = (Player) e.getPlayer();
				if(settingPlayer.contains(p.getName())) {
					if(e.getInventory().getTitle().equalsIgnoreCase(settingInvenTitle))
						applyRotationItemSetting(p, e.getInventory());
				}
			}	
		}
		
		@EventHandler
		public void onPlayerJoin(PlayerJoinEvent e) {
			if(isStart) {
				Player p = e.getPlayer();
				Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
					public void run() {
						applyItem(p);
					}
				}, 20l);		
			}
		}
		
		@EventHandler
		public void onDropItem(PlayerDropItemEvent e) {
			if(isStart) {
				Player p = e.getPlayer();
				ItemStack rotatingItem = rotationList.get(rotationIndex);
				ItemStack dropItem = e.getItemDrop().getItemStack();
				
				if(dropItem.getTypeId() == rotatingItem.getTypeId()
					&& dropItem.getType().getData() == rotatingItem.getType().getData()) {
					p.sendMessage(ms+"해당 아이템은 현재 버릴 수 없습니다.");
					e.setCancelled(true);
					p.updateInventory();
				}
			}
		}
		
		
		@EventHandler
		public void onClickInventory(InventoryClickEvent e) {
			//Bukkit.getLogger().info(lockItemSlot+" / "+e.getSlot()+" / "+e.getCurrentItem().getTypeId());
			if(isStart) {	
				if(e.getWhoClicked() instanceof Player) {
					Player p = (Player) e.getWhoClicked();
					if(e.getSlot() == lockItemSlot) {
						e.setCancelled(true);
						p.closeInventory();
						p.sendMessage(ms+"해당 슬롯은 현재 사용할 수 없습니다.");
						p.updateInventory();
					}
					
					
					/*List<Integer> clickMap = lastClickMap.get(p.getName());
					
					if(clickMap.contains(lockItemSlot)) {
						e.setCancelled(true);
					}
					
					for(int i = 0; i < 3; i++) {
						clickMap.set(i, clickMap.get(i+1));
					}
					clickMap.set(4, e.getSlot());*/
					
					/*ItemStack checkItem = p.getInventory().getItem(lockItemSlot);
					if(checkItem == null || checkItem.getTypeId() != rotatingItem.getTypeId()
							|| checkItem.getData() != rotatingItem.getData()) {
						//p.getInventory().setItem(lockItemSlot, rotatingItem);
						Bukkit.getLogger().info("재적용");
						p.updateInventory();
					}*/
				}
			}
		}
		
		@EventHandler
		public void onPlayerRespawn(PlayerRespawnEvent e) {
			if(isStart) {
				Player p = e.getPlayer();
				Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
					public void run() {
						applyItem(p);
					}
				}, 20l);		
			}
		}
		
		@EventHandler
		public void onPlayerDeath(PlayerDeathEvent e) {
			if(isStart) {
				Player p = e.getEntity();
				ItemStack rotatingItem = rotationList.get(rotationIndex);
				for(ItemStack item : e.getDrops()) {
					if(item.getTypeId() == rotatingItem.getTypeId()) {
						e.getDrops().remove(item);
					}							
				}
			}
		}
		
		@EventHandler
		public void onCommandInput(PlayerCommandPreprocessEvent e) {
			String cmdArgs[] = e.getMessage().split(" ");
			Player p = e.getPlayer();
			if(p.isOp()) {
				if(cmdArgs[0].equalsIgnoreCase("/시작")) {
					if(isStart) {
						p.sendMessage(ms+"이미 시작됐습니다.");
					} else {
						if(rotationList.size() < 2) {
							p.sendMessage(ms+"로테이션 아이템은 최소 2개 이상이여야합니다. '/설정' 으로 아이템을 먼저 설정하세요.");
						} else {
							if(cmdArgs.length < 2) {
								p.sendMessage(ms+"/시작 <시간초>");
							} else {
								int timeQuantum = 0;
								try {
									timeQuantum = Integer.valueOf(cmdArgs[1]);
								}catch (Exception excep) {
									p.sendMessage(ms+"시간초 부분에는 숫자만 입력해주세요.");
									return;
								}
								startRotation(timeQuantum);
								p.sendMessage(ms+"'/중지' 명령어로 로테이션을 중지할 수 있습니다.");
							}
						}
					}
					e.setCancelled(true);
				} else if(cmdArgs[0].equalsIgnoreCase("/설정")) {
					setRotationItem(p);
					p.sendMessage(ms+"아이템을 넣어주세요.");
					e.setCancelled(true);
				} else if(cmdArgs[0].equalsIgnoreCase("/중지")) {
					stopRotationItem(p);
					e.setCancelled(true);
				}
			}
		}
		
	}
	
	
}
