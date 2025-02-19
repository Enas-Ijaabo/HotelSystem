package root.reservation.service;

import root.permission.PermissionType;
import root.permission.dao.PermissionCostumerDAO;
import root.reservation.dao.ReservationDAO;
import root.reservation.room.Rooms;
import root.actors.Costumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Date;
import java.util.List;


public class ReservationService {
    @Autowired
    ReservationDAO reservationDAO;
    @Autowired
    Rooms rooms;
    @Autowired
    PermissionCostumerDAO permissionCostumerDAO;

    public ModelAndView reserve(String roomKind
            , HttpServletRequest request, ModelAndView modelAndView) {

        HttpSession session = request.getSession(true);

        Date fromDate=(Date)session.getAttribute("fromDate");
        Date toDate=(Date)session.getAttribute("toDate");

        if (!isAuthenticated(session)) {
            modelAndView.setViewName("Login");
            return modelAndView;
        }

        String roomNumber = reserveARoom(session, roomKind, fromDate, toDate);
        if (roomNumber.equals("") ) {

            String message = "<h1>Something went wrong</h1><br><br>"
                    + "<h3> The process of root.reservation is not completed!<br> " +
                    "go back to root.reservation <a href=/views/FindRoom.jsp> Find & Reserve room</a></h3>";

            modelAndView.addObject("message", message);

            modelAndView.setViewName("SomethingIsWrong");
            return modelAndView;
        }

        modelAndView.addObject("roomKind", roomKind);
        modelAndView.addObject("roomNumer", roomNumber);
        modelAndView.addObject("fromDate", fromDate);
        modelAndView.addObject("toDate", toDate);
        modelAndView.setViewName("ReservationConfirmed");

        return modelAndView;
    }


    //check if the costumer is logged in
    private boolean isAuthenticated(HttpSession session) {

        Costumer costumer = (Costumer) session.getAttribute("costumer");

        if (costumer == null) {
            return false;
        }

        if (costumer.getPermissions().contains(PermissionType.LOGGED_IN_COSTUMER)) {
            return true;
        }
        return false;
    }


    //reserve a room by registering the root.reservation info in the database
    private synchronized String reserveARoom(HttpSession session, String roomKind,
                                             Date fromDate, Date toDate) {

        Costumer costumer = (Costumer) session.getAttribute("costumer");
        String roomNumber = getAvailableRoomNum(fromDate, toDate, roomKind);
        if (roomNumber.equals("")){
            return roomNumber;
        }
        reservationDAO.insert(costumer, roomNumber, fromDate, toDate);
        costumer.setReservationIdRoomMap(reservationDAO.selectReservedRooms(costumer.getId()));
        addPermission(session);
        return roomNumber;

    }


    //return the Available room Number  from a certain type in a certain dates
    private String getAvailableRoomNum(Date fromDate, Date toDate, String roomKind) {

        List<String> availableRooms = reservationDAO.selectAvailableRooms(fromDate, toDate);
        String roomNum = "";

        for (String availableRoom : availableRooms) {
            String availableRoomKind = rooms.getNumberKindMap().get(availableRoom);

            if (availableRoomKind.equals(roomKind)) {

                roomNum = availableRoom;
                break;
            }
        }

        return roomNum;
    }


    //Add the root.permission to the costumer in both object and database
    private void addPermission(HttpSession session) {

        Costumer costumer = (Costumer) session.getAttribute("costumer");

        if (costumer.getPermissions().contains(PermissionType.HAS_A_RESERVATION))
        {
            return;
        }

        costumer.addPermission(PermissionType.HAS_A_RESERVATION);
        permissionCostumerDAO.insert(costumer.getId(), PermissionType.HAS_A_RESERVATION);
    }
}
