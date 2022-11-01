package gui

import cats.effect.Concurrent
import swing.*

import java.awt.Dimension
import javax.swing.*

class MainUI[F[+_]](
                    router: Router[F],
                    notifications: Delayed[F, NotificationScreen[F]],
                  ) extends Stateless, Window:

  override def view: View =
    new JFrame("Github helper") {
      setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
      setMinimumSize(new Dimension(300, 400))
      setResizable(false)
      setContentPane {
        new JPanel() {
          add(new JLabel("Welcome to GitHelper app."))
          add {
            new JButton("Notifications") {
              addActionListener { _ =>
                router.moveToScreen(notifications)
              }
            }
          }
        }
      }
    }
  end view

end MainUI
