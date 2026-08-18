package com.ninjaassemble.progression.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ninjaassemble.economy.application.WalletService;
import com.ninjaassemble.economy.domain.Currency;
import com.ninjaassemble.inventory.application.InventoryService;
import com.ninjaassemble.inventory.domain.InventoryStack;
import com.ninjaassemble.player.application.PlayerService;
import com.ninjaassemble.player.domain.PlayerEntity;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public final class AdvancedProgressionApplicationService {
    private final AdvancedProgressionCatalogService catalog; private final PlayerService players; private final WalletService wallet;
    private final InventoryService inventory; private final JdbcTemplate jdbc; private final ObjectMapper objectMapper;
    public AdvancedProgressionApplicationService(AdvancedProgressionCatalogService catalog, PlayerService players, WalletService wallet, InventoryService inventory, JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.catalog=catalog; this.players=players; this.wallet=wallet; this.inventory=inventory; this.jdbc=jdbc; this.objectMapper=objectMapper;
    }

    @Transactional(readOnly=true)
    public ProgressionBoard board(UUID playerId) {
        PlayerEntity player=players.require(playerId); Map<String,Integer> levels=new HashMap<>();
        jdbc.query("select track_id, level from player_progression_tracks where player_id=?", rs->{while(rs.next()) levels.put(rs.getString("track_id"),rs.getInt("level"));}, playerId);
        Map<String,Long> itemBalances=new HashMap<>(); for(InventoryStack stack:inventory.list(playerId)) itemBalances.put(stack.itemDefinitionId(),stack.quantity());
        long gold=wallet.getBalance(playerId,Currency.GOLD);
        List<TrackView> views=catalog.all().stream().map(t->{int level=levels.getOrDefault(t.id(),0);boolean maxed=level>=t.maxLevel();long goldCost=maxed?0:t.goldCost(level);long itemCost=maxed?0:t.itemCost(level);boolean unlocked=player.getAccountLevel()>=t.minPlayerLevel();long itemBalance=t.itemId().isBlank()?0:itemBalances.getOrDefault(t.itemId(),0L);boolean affordable=!maxed&&unlocked&&gold>=goldCost&&(t.itemId().isBlank()||itemBalance>=itemCost);String blocked=maxed?"MAX_LEVEL":!unlocked?"PLAYER_LEVEL":gold<goldCost?"GOLD":(!t.itemId().isBlank()&&itemBalance<itemCost)?"ITEM":"";return new TrackView(t.id(),t.type().name(),t.nameEn(),t.nameVi(),level,t.maxLevel(),t.minPlayerLevel(),unlocked,maxed,affordable,blocked,goldCost,t.itemId(),itemCost,t.bonusStat(),t.bonusPerLevel(),t.cumulativeBonus(level));}).toList();
        return new ProgressionBoard(AdvancedProgressionCatalogService.VERSION,player.getAccountLevel(),gold,views);
    }

    @Transactional
    public UpgradeResult upgrade(UUID playerId,String trackId,UUID requestId) {
        if(requestId==null) throw new IllegalArgumentException("requestId is required"); if(trackId==null||trackId.isBlank()) throw new IllegalArgumentException("trackId is required");
        long lockKey=requestId.getMostSignificantBits()^requestId.getLeastSignificantBits()^0x50524f4752455353L; jdbc.query("select pg_advisory_xact_lock(?)",rs->{},lockKey);
        UpgradeResult replay=loadRequest(requestId); if(replay!=null){if(!playerId.toString().equals(replay.playerId())||!trackId.equals(replay.trackId()))throw new IllegalStateException("progression requestId already belongs to another player/track");return replay.withReplayed(true);}
        PlayerEntity player=players.require(playerId); AdvancedProgressionCatalogService.TrackDefinition track=catalog.require(trackId); if(player.getAccountLevel()<track.minPlayerLevel())throw new IllegalStateException("progression track player-level requirement not met");
        jdbc.update("insert into player_progression_tracks(player_id,track_id,level) values (?,?,0) on conflict do nothing",playerId,trackId);
        Integer current=jdbc.queryForObject("select level from player_progression_tracks where player_id=? and track_id=? for update",Integer.class,playerId,trackId); int before=current==null?0:current; if(before>=track.maxLevel())throw new IllegalStateException("progression track is already max level");
        long goldCost=track.goldCost(before),itemCost=track.itemCost(before); long goldAfter=goldCost>0?wallet.mutate(playerId,Currency.GOLD,-goldCost,"PROGRESSION_UPGRADE",trackId,"progression:"+requestId+":gold"):wallet.getBalance(playerId,Currency.GOLD); long itemAfter=0;
        if(!track.itemId().isBlank()&&itemCost>0)itemAfter=inventory.mutate(playerId,track.itemId(),-itemCost,"PROGRESSION_UPGRADE","progression:"+requestId+":item:"+track.itemId()).quantity();
        int after=before+1; jdbc.update("update player_progression_tracks set level=?,updated_at=now() where player_id=? and track_id=?",after,playerId,trackId);
        UpgradeResult result=new UpgradeResult(requestId.toString(),playerId.toString(),trackId,AdvancedProgressionCatalogService.VERSION,false,before,after,goldCost,goldAfter,track.itemId().isBlank()?null:track.itemId(),itemCost,itemAfter,track.bonusStat(),track.cumulativeBonus(after));persistRequest(requestId,playerId,trackId,result);return result;
    }

    private UpgradeResult loadRequest(UUID requestId){List<String> rows=jdbc.query("select result_json from progression_upgrade_requests where request_id=?",(rs,row)->rs.getString(1),requestId);if(rows.isEmpty())return null;try{return objectMapper.readValue(rows.get(0),UpgradeResult.class);}catch(JsonProcessingException e){throw new IllegalStateException("cannot decode progression request",e);}}
    private void persistRequest(UUID requestId,UUID playerId,String trackId,UpgradeResult value){try{jdbc.update("insert into progression_upgrade_requests(request_id,player_id,track_id,result_json) values (?,?,?,?)",requestId,playerId,trackId,objectMapper.writeValueAsString(value));}catch(JsonProcessingException e){throw new IllegalStateException("cannot encode progression request",e);}}

    public record ProgressionBoard(String catalogVersion,int playerLevel,long gold,List<TrackView> tracks){}
    public record TrackView(String trackId,String trackType,String nameEn,String nameVi,int level,int maxLevel,int minPlayerLevel,boolean unlocked,boolean maxed,boolean affordable,String blockedReason,long nextGoldCost,String itemId,long nextItemCost,String bonusStat,int bonusPerLevel,int cumulativeBonus){}
    public record UpgradeResult(String requestId,String playerId,String trackId,String catalogVersion,boolean replayed,int levelBefore,int levelAfter,long goldCost,long goldAfter,String itemId,long itemCost,long itemAfter,String bonusStat,int cumulativeBonus){public UpgradeResult withReplayed(boolean value){return new UpgradeResult(requestId,playerId,trackId,catalogVersion,value,levelBefore,levelAfter,goldCost,goldAfter,itemId,itemCost,itemAfter,bonusStat,cumulativeBonus);}}
}
