import { Navigate, Route, Routes } from 'react-router-dom'
import BookingPage from './pages/BookingPage'
import BookingFormPage from './pages/BookingFormPage'
import BookingDonePage from './pages/BookingDonePage'
import MyReservationsPage from './pages/MyReservationsPage'
import AdminPage from './pages/AdminPage'

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<BookingPage />} />
      <Route path="/reserve" element={<BookingFormPage />} />
      <Route path="/done" element={<BookingDonePage />} />
      <Route path="/my" element={<MyReservationsPage />} />
      <Route path="/admin" element={<AdminPage />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
