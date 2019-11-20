package com.codeoftheweb.salvo.Controller;


import com.codeoftheweb.salvo.Model.*;
import com.codeoftheweb.salvo.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api")
public class SalvoController {
    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private GamePlayerRepository gamePlayerRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private ShipRepository shipRepository;

    @Autowired
    private SalvoRepository salvoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @RequestMapping("/games")
    public Map<String, Object> getAll(Authentication authentication){
        Map<String, Object> dto = new LinkedHashMap<>();
        if(!isGuest(authentication)){
            dto.put("player", playerRepository.findByUsername(authentication.getName()).makePlayerDto());
        }else{
            dto.put("player", null);
        }
        dto.put("games", gameRepository.findAll().stream()
                .map(game -> game.makeGameDto()).collect(Collectors.toList()));
        return dto;
    }
    private boolean isGuest(Authentication authentication) {
        return authentication == null || authentication instanceof AnonymousAuthenticationToken;
    }
    //*************************************************************************************
    @RequestMapping(path="/games/{id}/players", method = RequestMethod.GET)
    public Map<String, Object> getPlayersInGame(Authentication authentication, @PathVariable long id){
        Map<String, Object> dto = new LinkedHashMap<>();
        if(!isGuest(authentication)){
            dto.put("playerLoggedIn", playerRepository.findByUsername(authentication.getName()).makePlayerDto());
        }else{
            dto.put("playerLoggedIn", null);
        }
        dto.put("gamePlayers", gameRepository.getOne(id).getGamePlayers().stream()
                .map(player -> player.makeGamePlayerDto()).collect(Collectors.toList()));
        return dto;
    }
    //**************************************Ships of a player**************************************
    @RequestMapping(path = "/games/players/{gamePlayerId}/ships", method = RequestMethod.GET)
    public Map<String, Object> getShipsPlayer (Authentication authentication, @PathVariable long gamePlayerId){
        Map<String, Object> dto = new LinkedHashMap<>();
        if(!isGuest(authentication)){
            dto.put("playerLoggedIn", playerRepository.findByUsername(authentication.getName()).makePlayerDto());
        }else{
            dto.put("playerLoggedIn", null);
        }
        GamePlayer gamePlayer = gamePlayerRepository.getOne(gamePlayerId);
        dto.put("ships", getShipList(gamePlayer.getShips()));
        return dto;
    }

    //************************************Salvoes of a player*************************************
    @RequestMapping(path = "/games/players/{gamePlayerId}/salvoes", method = RequestMethod.GET)
    public Map<String, Object> getSalvoesPlayer(Authentication authentication, @PathVariable long gamePlayerId){
        Map<String, Object> dto = new LinkedHashMap<>();
        if(!isGuest(authentication)){
            dto.put("playerLoggedIn", playerRepository.findByUsername(authentication.getName()).makePlayerDto());
        }else{
            dto.put("playerLoggedIn", null);
        }
        GamePlayer gamePlayer = gamePlayerRepository.getOne(gamePlayerId);
        dto.put("salvoes", gamePlayer.getSalvoes().stream().map(salvo -> salvo.makeSalvoDto()));
        return dto;
    }



    @RequestMapping("/game_view/{gamePlayerId}")
    public ResponseEntity gameView(@PathVariable long gamePlayerId, Authentication authentication){
        GamePlayer gamePlayer = gamePlayerRepository.getOne(gamePlayerId);

        if(gamePlayer.getPlayer().getId() == playerRepository.findByUsername(authentication.getName()).getId()){
            return ResponseEntity.ok(gameViewDto(gamePlayer));
        }else{
            ResponseEntity responseEntity = new ResponseEntity(HttpStatus.FORBIDDEN);
            return responseEntity;
        }
    }
    private Map<String, Object> gameViewDto(GamePlayer gamePlayer){
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", gamePlayer.getGame().getId());
        dto.put("created", gamePlayer.getGame().getCreationDate());
        dto.put("gamePlayers", gamePlayer.getGame().getGamePlayers().stream().map(g ->g.makeGamePlayerDto()));
        dto.put("ships", getShipList(gamePlayer.getShips()));
        dto.put("salvoes", gamePlayer.getGame().getGamePlayers().stream()
                                                                .flatMap(gp->gp.getSalvoes()
                                                                                .stream()
                                                                                .map(salvo -> salvo.makeSalvoDto())
                                                                        )
                                                                .collect(Collectors.toList())
        );
        return dto;
    }
    public List<Map<String, Object>> getShipList(Set<Ship> ships){
        return ships.stream().map(ship-> ship.makeShipDto()).collect(Collectors.toList());
    }

    @RequestMapping("/leaderboard")
    public List<Object> leaderboardView(){
        return playerRepository.findAll().stream().map(player -> player.makeTableLeaderboard()).collect(Collectors.toList());
    }
    //********************************************  Create a player  *************************************************
    @RequestMapping(path = "/players", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> createUser(@RequestParam String username, @RequestParam String password, Authentication authentication){
        if(!isGuest(authentication)){
            return new ResponseEntity<>(makeMap("error", "User logged in"), HttpStatus.CONFLICT);
        }
        else if(username.isEmpty()){
            return new ResponseEntity<>(makeMap("error", "No name given"), HttpStatus.FORBIDDEN);
        }
        else if(password.isEmpty()){
            return new ResponseEntity<>(makeMap("error", "No password given"), HttpStatus.FORBIDDEN);
        }
        else if(playerRepository.findByUsername(username) != null){
            return new ResponseEntity<>(makeMap("error","Username already exists"), HttpStatus.CONFLICT);
        }
        else {
            Player newPlayer = playerRepository.save(new Player(username, passwordEncoder.encode(password)));
            return new ResponseEntity<>(makeMap("username:", newPlayer.getUsername()), HttpStatus.CREATED);
        }
    }
    //***************************************  Create a game  ***************************************************
    @RequestMapping(path = "/games", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> createGame(Authentication authentication){
        if(isGuest(authentication)){
            return new ResponseEntity<>(makeMap("error:", "No player logged in"), HttpStatus.UNAUTHORIZED);
        }
        Player player = playerRepository.findByUsername(authentication.getName());
        Game newGame = gameRepository.save(new Game(0));
        GamePlayer newGamePlayer = gamePlayerRepository.save(new GamePlayer(newGame, player));
        return new ResponseEntity<>(makeMap("gpid", newGamePlayer.getId()),HttpStatus.CREATED);
    }
    //**************************************  Join Game  *******************************************************
    @RequestMapping(path = "/games/{id}/players", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> joinGame(Authentication authentication, @PathVariable long id){
        if(isGuest(authentication)){
            return new ResponseEntity<>(makeMap("error", "No player logged in"), HttpStatus.UNAUTHORIZED);
        }
        Game game = gameRepository.findById(id).orElse(null);
        if(game == null){
            return new ResponseEntity<>(makeMap("error", "No such Game"), HttpStatus.FORBIDDEN);
        }
        Player player = playerRepository.findByUsername(authentication.getName());
        if(game.getGamePlayers().stream().map(pl ->pl.getPlayer().getUsername()).collect(Collectors.toList()).contains(player.getUsername())){
            return new ResponseEntity<>(makeMap("error", "You are already a player in this game"), HttpStatus.FORBIDDEN);
        }
        if(game.getGamePlayers().size() >= 2){
            return new ResponseEntity<>(makeMap("error", "Game is full"), HttpStatus.FORBIDDEN);
        }

        GamePlayer newGamePlayer = gamePlayerRepository.save(new GamePlayer(game, player));
        return new ResponseEntity<>(makeMap("gpid", newGamePlayer.getId()), HttpStatus.CREATED);
    }
    //*****************************************Add ship*********************************************************************
    @RequestMapping(path = "/games/players/{gamePlayerId}/ships",produces = "application/json", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> addShips (@PathVariable long gamePlayerId,
                                                        @RequestBody List<ShipPlayer> newShips,
                                                        Authentication authentication){
        GamePlayer gamePlayer = gamePlayerRepository.findById(gamePlayerId).orElse(null);
        Player player = playerRepository.findByUsername(authentication.getName());

        if(isGuest(authentication) || gamePlayer == null || player.getId() != gamePlayer.getPlayer().getId()){
            return new ResponseEntity<>(makeMap("error", "No player logged in"), HttpStatus.UNAUTHORIZED);
        }
        if(!gamePlayer.getShips().isEmpty()){
            return new ResponseEntity<>(makeMap("error", "Player already place ships"), HttpStatus.FORBIDDEN);
        }
        newShips.stream().forEach(ship -> shipRepository.save(new Ship(gamePlayer, ship.getType(), ship.getShipLocations())));
        return new ResponseEntity<>(makeMap("done", "Ship added"), HttpStatus.CREATED);
    }

    //********************************Add salvo***************************************************
    @RequestMapping(path = "/games/players/{gamePlayerId}/salvoes", produces = "application/json", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> addSalvoes(@PathVariable long gamePlayerId,
                                                         @RequestBody SalvoPlayer newSalvoes,
                                                          Authentication authentication){
        GamePlayer gamePlayer = gamePlayerRepository.findById(gamePlayerId).orElse(null);
        Player player = playerRepository.findByUsername(authentication.getName());
        if(isGuest(authentication) || gamePlayer == null || player.getId() != gamePlayer.getPlayer().getId()){
            return new ResponseEntity<>(makeMap("error", "No player logged in"), HttpStatus.UNAUTHORIZED);
        }
        List<Object> salvoes = gamePlayer.getSalvoes().stream().map(salvo -> salvo.makeSalvoDto()).collect(Collectors.toList());
        List<Object> salvoTurn = salvoes.stream().filter(salvo -> salvo.)
        if(salvoes.size()>0){
            return new ResponseEntity<>(makeMap("error", "Player already fire salvoes this turn"), HttpStatus.FORBIDDEN);
        }
        Salvo salvo = salvoRepository.save(new Salvo(gamePlayer, newSalvoes.getTurn(), newSalvoes.getSalvoLocations()));
        return new ResponseEntity<>(makeMap("done", "salvo added"), HttpStatus.CREATED);
    }
    private Map<String, Object> makeMap (String key, Object value){
        Map<String, Object>  map = new HashMap<>();
        map.put(key, value);
        return map;
    }
}
