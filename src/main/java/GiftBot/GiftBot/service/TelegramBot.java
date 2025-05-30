package GiftBot.GiftBot.service;

import GiftBot.GiftBot.config.BotConfig;
import GiftBot.GiftBot.exception.NotFoundException;
import GiftBot.GiftBot.gift.Gift;
import GiftBot.GiftBot.gift.GiftRepository;
import GiftBot.GiftBot.giftBookings.GiftBookings;
import GiftBot.GiftBot.giftBookings.GiftBookingsRepository;
import GiftBot.GiftBot.user.User;
import GiftBot.GiftBot.user.UserRepository;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {

    private final BotConfig botConfig;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GiftRepository giftRepository;

    @Autowired
    private GiftBookingsRepository giftBookingsRepository;

    static final String HELP_TEXT = "WishListSHBot создан для облегчения выбора подарка для друга. \n\n" +
            "Для выбора команды можно использовать меню в левой нижней части экрана \n\n" +
            "или напечатать команду текстом: \n\n" +
            "Список доступных команд:\n\n" +
            "/start \n\n" +
            "/help \n\n" +
            "/Выбрать друга \n\n" +
            "/Создать Wish List";

    static final String ERROR_TEXT = "Error occurred: ";
    static final String FRIEND_BUTTON = "FRIEND_BUTTON";
    static final String COMMAND_FRIENDS_GIFTS = "FRIENDS_GIFTS";
    static final String CREATE_LIST_BUTTON = "CREATE_LIST_BUTTON";
    static final String GET_GIFTS_BUTTONS = "GET_GIFTS_BUTTONS";
    static final String CHOOSE_GIFT = "CHOOSE_GIFT";
    static final String NO_BUTTON = "NO_BUTTON";
    static final String YES_BUTTON = "YES_BUTTON";
    static final String BOOKING_BUTTON = "BOOKING_BUTTON";

    private Map<Long, Boolean> waitingForResponse = new HashMap<>();
    private String command = "";

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    public TelegramBot(BotConfig botConfig) {
        this.botConfig = botConfig;

        List<BotCommand> listOfCommands = new ArrayList<>();

        listOfCommands.add(new BotCommand("/start", "Старт"));
        listOfCommands.add(new BotCommand("/help", "Инструкция по использованию бота"));
        listOfCommands.add(new BotCommand("/wish_list", "Посмотреть мой список желаний."));

        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Ошибка установки меню бота. {}", e.getMessage());
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (messageText.contains("/send") && botConfig.getOwnerId() == chatId) {
                var textToSend = EmojiParser.parseToUnicode(messageText.substring(messageText.indexOf(" ")));
                var users = userRepository.findAll();

                for (User user : users) {
                    prepareAndSendMessage(user.getId(), textToSend);
                }
            } else if (command.equals(COMMAND_FRIENDS_GIFTS)) {
                String friendUserName = messageText;
                //метод выводящий список подарков как кнопки переходящие на сайт;
                List<Gift> gifts = getAvailableFriendsGifts(friendUserName);
                if (gifts.isEmpty()) {
                    prepareAndSendMessage(chatId, "Доступных подарков сейчас нет.");
                } else {
                    chooseGiftsButton(gifts, chatId, friendUserName);
                }
                command = "";

            } else {

                switch (messageText) {
                    case "/start":
                        registerUser(update.getMessage());
                        log.info("Пользователь {} зарегестрирован.", update.getMessage().getChat().getFirstName());
                        startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                        break;
                    case "/help":
                        prepareAndSendMessage(chatId, HELP_TEXT);
                        break;
                    case "/Выбрать друга;":

                        break;
                    case "/Создать список желаний":
                       
                        //метод для создания подарков и добалвения их в список
                        break;
                    case "/wish_list":

                        break;
                    default:
                        prepareAndSendMessage(chatId, "Извини, эта команда еще не поддерживатеся.");
                }
            }
        } else if (update.hasCallbackQuery()) {
            String callBackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (callBackData.equals(FRIEND_BUTTON)) {
                //String text = "Список друзей появится ниже, выбери одного из списка и введи его username";
                getFriendList(chatId, messageId);
                command = COMMAND_FRIENDS_GIFTS;

                //executeMessageEditText(text, chatId, messageId);
            } else if (callBackData.equals(CREATE_LIST_BUTTON)) {
                String text = "Введине название подарка и ссылку на него";
                //тут метод приема в список подарков
                executeMessageEditText(text, chatId, messageId);

            } else if (callBackData.split("-")[0].equals(CHOOSE_GIFT)) {

                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("Для просмотра подарка по ссылке нажми на правую кнопу \n" +
                        "Для брони этого подарка нажми на левую кнопку");

                InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

                List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();

                List<InlineKeyboardButton> rowInLine = new ArrayList<>();

                var bookingButton = new InlineKeyboardButton();
                bookingButton.setText("Забронировать");
                bookingButton.setCallbackData(BOOKING_BUTTON + "-" + callBackData);

                var linkButton = new InlineKeyboardButton();

                linkButton.setText("Ссылка на подарок");

                String url = giftRepository.findByName(callBackData.split("-")[1]).get().getLink();
                linkButton.setUrl(url);

                rowInLine.add(bookingButton);
                rowInLine.add(linkButton);

                rowsInLine.add(rowInLine);

                inlineKeyboardMarkup.setKeyboard(rowsInLine);

                message.setReplyMarkup(inlineKeyboardMarkup);

                executeMessage(message);

            } else if (callBackData.split("-")[0].equals(BOOKING_BUTTON)) {
                //Тут Метод который выбрасывает еще 2 кнопки (Забронировать и Перейти на сайт)

                String friendName = callBackData.split("-")[3];
                User friend = userRepository.findByUserName(friendName)
                        .orElseThrow(() -> new NotFoundException("Пользователь не найден по userName"));

                Gift gift = giftRepository.findByName(callBackData.split("-")[2])
                        .orElseThrow(() -> new NotFoundException("Подарок не найден по иимени"));

                gift.setAvailable(false);

                giftRepository.save(gift);
                GiftBookings giftBooking = new GiftBookings();
                giftBooking.setFriendId(friend.getId());
                giftBooking.setUserId(chatId);
                giftBooking.setGiftId(gift.getId());


                giftBookingsRepository.save(giftBooking);

                String text = "Подарок: " + gift.getName() + " забронирован за вами.";

                executeMessageEditText(text, chatId, messageId);
            }
        }
    }

    private List<Gift> getAvailableFriendsGifts(String friendUserName) {
        return userRepository.findUsersGiftsByAvailable(true, friendUserName);
    }

    private void chooseGiftsButton(List<Gift> gifts, long chatId, String friendUserName) {

        String text = "Нажми на один из подарков в спискке для выбора.";

        Map<String, String> giftsWithLink = new HashMap<>();
        gifts.forEach(g -> {
            giftsWithLink.put(g.getName(), g.getLink());
        });


        InlineKeyboardMarkup prepareInlineKeyboardMarkup = keyboardButtonForGiftsCreator(giftsWithLink, friendUserName);

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        message.setReplyMarkup(prepareInlineKeyboardMarkup);
        executeMessage(message);
    }


    private void getFriendList(long chatId, long messageId) {
        List<User> friends = userRepository.findAllByIdNot(chatId).get();

        if (friends.isEmpty()) {
            log.info("Список друзей пуст;");
            executeMessageEditText("Список друзей пуст", chatId, messageId);
        } else {

            String text = "Список друзей появится ниже, выбери одного из списка и введи его username";
            executeMessageEditText(text, chatId, messageId);

            List<String> friendsList = new ArrayList<>();

            friends.stream().forEach(f -> friendsList.add(f.getUserName()));

            log.info("В списке друзей есть люди.");
            executeMessageEditText(friendsList, chatId, messageId);
        }
    }

    private void startCommandReceived(long chatId, String name) {

        String answer = EmojiParser.parseToUnicode("Привет " + name + "!" + " :blush: \n\n" +
                ":gift: В этом боте ты можешь: \n\n" +
                "- создать свой список желаний. Нажми «Создать список желаний»\n\n" +
                "- выбрать подарок другу из его списка. Нажми «Выбрать друга»"
        );

        log.info("Ответ пользователю {}", name);

        startMessage(chatId, answer);
    }

    private InlineKeyboardMarkup keyboardButtonForGiftsCreator(Map<String, String> buttonsWithLink, String friendUserName) {

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();

        for (String giftName : buttonsWithLink.keySet()) {
            var button = new InlineKeyboardButton();

            button.setText(giftName);
            //button.setUrl(buttonsWithLink.get(giftName));
            String url = buttonsWithLink.get(giftName);
            button.setCallbackData(CHOOSE_GIFT + "-" + giftName + "-" + friendUserName);

            List<InlineKeyboardButton> rowInLine = new ArrayList<>();
            rowInLine.add(button);

            rowsInLine.add(rowInLine);
        }

        inlineKeyboardMarkup.setKeyboard(rowsInLine);

        return inlineKeyboardMarkup;
    }

    private void startMessage(long chatId, String messageText) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(messageText);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();

        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        var friendButton = new InlineKeyboardButton();

        friendButton.setText("Выбрать друга");
        friendButton.setCallbackData(FRIEND_BUTTON);

        var createListButton = new InlineKeyboardButton();

        createListButton.setText("Создать список желаний");
        createListButton.setCallbackData(CREATE_LIST_BUTTON);

        rowInLine.add(friendButton);
        rowInLine.add(createListButton);

        rowsInLine.add(rowInLine);

        inlineKeyboardMarkup.setKeyboard(rowsInLine);

        message.setReplyMarkup(inlineKeyboardMarkup);

        executeMessage(message);
    }

    private void prepareAndSendMessage(Long chatId, String textToSend) {
        SendMessage message = new SendMessage();

        message.setChatId(chatId);
        message.setText(textToSend);

        executeMessage(message);
    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }

    private void registerUser(Message message) {

        if (userRepository.findById(message.getChatId()).isEmpty()) {
            var chatId = message.getChatId();
            var chat = message.getChat();

            User user = new User();

            user.setId(chatId);
            user.setName(chat.getFirstName());
            user.setUserName(chat.getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);
            log.info("User saved: {}", user);
        }
    }

    private void executeMessageEditText(String text, long chatId, long messageId) {
        EditMessageText message = new EditMessageText();
        message.setChatId(chatId);
        message.setText(text);
        message.setMessageId((int) messageId);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }

    private void executeMessageEditText(List<String> listOfText, long chatId, long messageId) {
        SendMessage message = new SendMessage();

        StringBuilder text = new StringBuilder();
        listOfText.stream().forEach(t -> text.append(t + "\n"));

        message.setChatId(chatId);
        message.setText(String.valueOf(text));
        executeMessage(message);
    }
}
